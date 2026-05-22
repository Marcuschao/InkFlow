package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.common.support.PageResult;
import com.blog.personalblogbackend.mapper.ArticleFavoriteMapper;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.ArticleFavorite;
import com.blog.personalblogbackend.model.vo.ArticleVO;
import com.blog.personalblogbackend.model.vo.interaction.FavoriteStatusVo;
import com.blog.personalblogbackend.service.ArticleFavoriteService;
import com.blog.personalblogbackend.service.ArticleInteractionEnricher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleFavoriteServiceImpl implements ArticleFavoriteService {

    @Autowired
    private ArticleFavoriteMapper articleFavoriteMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private ArticleInteractionEnricher articleInteractionEnricher;

    private void requirePublished(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            throw new ServiceException(404, "文章不存在或未发布");
        }
    }

    @Override
    @Transactional
    public FavoriteStatusVo toggle(Long userId, Long articleId) {
        requirePublished(articleId);
        if (isFavorited(userId, articleId)) {
            articleFavoriteMapper.delete(new LambdaQueryWrapper<ArticleFavorite>()
                    .eq(ArticleFavorite::getUserId, userId)
                    .eq(ArticleFavorite::getArticleId, articleId));
            return new FavoriteStatusVo(false);
        }
        ArticleFavorite fav = new ArticleFavorite();
        fav.setUserId(userId);
        fav.setArticleId(articleId);
        fav.setCreateTime(LocalDateTime.now());
        articleFavoriteMapper.insert(fav);
        return new FavoriteStatusVo(true);
    }

    @Override
    public FavoriteStatusVo status(Long userId, Long articleId) {
        if (articleMapper.selectById(articleId) == null) {
            throw new ServiceException(404, "文章不存在");
        }
        return new FavoriteStatusVo(isFavorited(userId, articleId));
    }

    @Override
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
        return articleFavoriteMapper.selectCount(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getUserId, userId)
                .eq(ArticleFavorite::getArticleId, articleId)) > 0;
    }
}
