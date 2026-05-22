package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.mapper.ArticleLikeMapper;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.ArticleLike;
import com.blog.personalblogbackend.model.vo.interaction.LikeCountVo;
import com.blog.personalblogbackend.model.vo.interaction.LikeStatusVo;
import com.blog.personalblogbackend.service.ArticleLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ArticleLikeServiceImpl implements ArticleLikeService {

    @Autowired
    private ArticleLikeMapper articleLikeMapper;
    @Autowired
    private ArticleMapper articleMapper;

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
    @Transactional
    public LikeStatusVo toggle(Long userId, Long articleId) {
        Article article = requirePublished(articleId);
        boolean exists = isLiked(userId, articleId);
        if (exists) {
            articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getUserId, userId)
                    .eq(ArticleLike::getArticleId, articleId));
            articleMapper.decrementLikeCount(articleId);
            Article fresh = articleMapper.selectById(articleId);
            return new LikeStatusVo(false, likeCountOf(fresh));
        }
        ArticleLike like = new ArticleLike();
        like.setUserId(userId);
        like.setArticleId(articleId);
        like.setCreateTime(LocalDateTime.now());
        articleLikeMapper.insert(like);
        articleMapper.incrementLikeCount(articleId);
        Article fresh = articleMapper.selectById(articleId);
        return new LikeStatusVo(true, likeCountOf(fresh));
    }

    @Override
    public LikeStatusVo status(Long userId, Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException(404, "文章不存在");
        }
        return new LikeStatusVo(isLiked(userId, articleId), likeCountOf(article));
    }

    @Override
    public LikeCountVo publicCount(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException(404, "文章不存在");
        }
        return new LikeCountVo(likeCountOf(article));
    }

    @Override
    public boolean isLiked(Long userId, Long articleId) {
        if (userId == null) {
            return false;
        }
        return articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getArticleId, articleId)) > 0;
    }
}
