package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.common.support.PageResult;
import com.blog.personalblogbackend.idempotency.WriteIdempotencyService;
import com.blog.personalblogbackend.interaction.InteractionRedisStore;
import com.blog.personalblogbackend.mapper.ArticleFavoriteMapper;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.messaging.InteractionPersistMessage;
import com.blog.personalblogbackend.messaging.InteractionProducer;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.ArticleFavorite;
import com.blog.personalblogbackend.model.vo.ArticleVO;
import com.blog.personalblogbackend.model.vo.interaction.FavoriteStatusVo;
import com.blog.personalblogbackend.service.ArticleFavoriteService;
import com.blog.personalblogbackend.service.ArticleInteractionEnricher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleFavoriteServiceImpl implements ArticleFavoriteService {

    private static final String IDEM_SCOPE = "favorite";

    private final ArticleFavoriteMapper articleFavoriteMapper;
    private final ArticleMapper articleMapper;
    private final ArticleInteractionEnricher articleInteractionEnricher;
    private final WriteIdempotencyService idempotencyService;
    private final InteractionRedisStore interactionRedisStore;
    private final InteractionProducer interactionProducer;

    public ArticleFavoriteServiceImpl(ArticleFavoriteMapper articleFavoriteMapper,
                                      ArticleMapper articleMapper,
                                      ArticleInteractionEnricher articleInteractionEnricher,
                                      WriteIdempotencyService idempotencyService,
                                      InteractionRedisStore interactionRedisStore,
                                      InteractionProducer interactionProducer) {
        this.articleFavoriteMapper = articleFavoriteMapper;
        this.articleMapper = articleMapper;
        this.articleInteractionEnricher = articleInteractionEnricher;
        this.idempotencyService = idempotencyService;
        this.interactionRedisStore = interactionRedisStore;
        this.interactionProducer = interactionProducer;
    }

    private Article requirePublished(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            throw new ServiceException(404, "文章不存在或未发布");
        }
        return article;
    }

    @Override
    public FavoriteStatusVo toggle(Long userId, Long articleId, String idempotencyKey) {
        requirePublished(articleId);
        if (StringUtils.hasText(idempotencyKey)) {
            String key = WriteIdempotencyService.resolveKey(idempotencyKey, userId + ":fav:" + articleId);
            return idempotencyService.execute(IDEM_SCOPE, key, FavoriteStatusVo.class,
                    () -> doToggle(userId, articleId));
        }
        return doToggle(userId, articleId);
    }

    private FavoriteStatusVo doToggle(Long userId, Long articleId) {
        boolean currently = isFavorited(userId, articleId);
        boolean favorited = !currently;
        if (interactionRedisStore.redisEnabled()) {
            interactionRedisStore.setFavorited(userId, articleId, favorited);
        } else {
            if (favorited) {
                ArticleFavorite fav = new ArticleFavorite();
                fav.setUserId(userId);
                fav.setArticleId(articleId);
                fav.setCreateTime(LocalDateTime.now());
                articleFavoriteMapper.insert(fav);
            } else {
                articleFavoriteMapper.delete(new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getUserId, userId)
                        .eq(ArticleFavorite::getArticleId, articleId));
            }
        }
        InteractionPersistMessage msg = new InteractionPersistMessage();
        msg.setAction(InteractionPersistMessage.FAVORITE_TOGGLE);
        msg.setUserId(userId);
        msg.setArticleId(articleId);
        msg.setFavorited(favorited);
        interactionProducer.publish(msg);
        return new FavoriteStatusVo(favorited);
    }

    @Override
    public FavoriteStatusVo status(Long userId, Long articleId) {
        if (articleMapper.selectById(articleId) == null) {
            throw new ServiceException(404, "文章不存在");
        }
        return new FavoriteStatusVo(isFavorited(userId, articleId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ArticleVO> listMine(Long userId, int page, int size) {
        long offset = (long) (page - 1) * size;
        List<Article> list = articleFavoriteMapper.selectFavoriteArticles(userId, offset, size);
        long total = articleFavoriteMapper.countFavoriteArticles(userId);
        List<ArticleVO> vos = list.stream()
                .map(articleInteractionEnricher::toSummaryVo)
                .peek(vo -> articleInteractionEnricher.enrich(vo, userId))
                .collect(Collectors.toList());
        return PageResult.build(vos, total, size, page);
    }

    @Override
    public boolean isFavorited(Long userId, Long articleId) {
        if (userId == null) {
            return false;
        }
        Boolean cached = interactionRedisStore.isFavoritedCached(userId, articleId);
        if (cached != null) {
            return cached;
        }
        boolean db = articleFavoriteMapper.selectCount(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getUserId, userId)
                .eq(ArticleFavorite::getArticleId, articleId)) > 0;
        interactionRedisStore.setFavorited(userId, articleId, db);
        return db;
    }
}
