package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.idempotency.WriteIdempotencyService;
import com.blog.personalblogbackend.interaction.InteractionRedisStore;
import com.blog.personalblogbackend.mapper.ArticleLikeMapper;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.messaging.InteractionPersistMessage;
import com.blog.personalblogbackend.messaging.InteractionProducer;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.ArticleLike;
import com.blog.personalblogbackend.model.vo.interaction.LikeCountVo;
import com.blog.personalblogbackend.model.vo.interaction.LikeStatusVo;
import com.blog.personalblogbackend.service.ArticleLikeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArticleLikeServiceImpl implements ArticleLikeService {

    private static final String IDEM_SCOPE = "like";

    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleMapper articleMapper;
    private final WriteIdempotencyService idempotencyService;
    private final InteractionRedisStore interactionRedisStore;
    private final InteractionProducer interactionProducer;

    public ArticleLikeServiceImpl(ArticleLikeMapper articleLikeMapper,
                                    ArticleMapper articleMapper,
                                    WriteIdempotencyService idempotencyService,
                                    InteractionRedisStore interactionRedisStore,
                                    InteractionProducer interactionProducer) {
        this.articleLikeMapper = articleLikeMapper;
        this.articleMapper = articleMapper;
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

    private int likeCountOf(Article article) {
        return article.getLikeCount() != null ? article.getLikeCount() : 0;
    }

    @Override
    public LikeStatusVo toggle(Long userId, Long articleId, String idempotencyKey) {
        requirePublished(articleId);
        if (StringUtils.hasText(idempotencyKey)) {
            String key = WriteIdempotencyService.resolveKey(idempotencyKey,
                    userId + ":like:" + articleId);
            return idempotencyService.execute(IDEM_SCOPE, key, LikeStatusVo.class,
                    () -> doToggle(userId, articleId));
        }
        return doToggle(userId, articleId);
    }

    private LikeStatusVo doToggle(Long userId, Long articleId) {
        boolean currentlyLiked = isLiked(userId, articleId);
        InteractionRedisStore.LikeToggleResult toggle = interactionRedisStore.toggleLike(
                userId, articleId, currentlyLiked);
        int count;
        if (interactionRedisStore.redisEnabled()) {
            count = toggle.count();
        } else {
            if (toggle.liked()) {
                articleLikeMapper.insert(buildLike(userId, articleId));
                articleMapper.incrementLikeCount(articleId);
            } else {
                articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getUserId, userId)
                        .eq(ArticleLike::getArticleId, articleId));
                articleMapper.decrementLikeCount(articleId);
            }
            Article fresh = articleMapper.selectById(articleId);
            count = likeCountOf(fresh);
        }

        InteractionPersistMessage msg = new InteractionPersistMessage();
        msg.setAction(InteractionPersistMessage.LIKE_TOGGLE);
        msg.setUserId(userId);
        msg.setArticleId(articleId);
        msg.setLiked(toggle.liked());
        interactionProducer.publish(msg);

        return new LikeStatusVo(toggle.liked(), count);
    }

    private static ArticleLike buildLike(Long userId, Long articleId) {
        ArticleLike like = new ArticleLike();
        like.setUserId(userId);
        like.setArticleId(articleId);
        like.setCreateTime(java.time.LocalDateTime.now());
        return like;
    }

    @Override
    public LikeStatusVo status(Long userId, Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException(404, "文章不存在");
        }
        return new LikeStatusVo(isLiked(userId, articleId), resolveCount(articleId, article));
    }

    @Override
    public LikeCountVo publicCount(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException(404, "文章不存在");
        }
        return new LikeCountVo(resolveCount(articleId, article));
    }

    private int resolveCount(Long articleId, Article article) {
        Integer cached = interactionRedisStore.getLikeCountCached(articleId);
        if (cached != null) {
            return cached;
        }
        int count = likeCountOf(article);
        interactionRedisStore.seedLikeCount(articleId, count);
        return count;
    }

    @Override
    public boolean isLiked(Long userId, Long articleId) {
        if (userId == null) {
            return false;
        }
        Boolean cached = interactionRedisStore.isLikedCached(userId, articleId);
        if (cached != null) {
            return cached;
        }
        boolean db = articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getArticleId, articleId)) > 0;
        interactionRedisStore.setLiked(userId, articleId, db);
        return db;
    }
}
