package com.blog.personalblogbackend.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.interaction.InteractionRedisStore;
import com.blog.personalblogbackend.mapper.ArticleFavoriteMapper;
import com.blog.personalblogbackend.mapper.ArticleLikeMapper;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.ArticleFavorite;
import com.blog.personalblogbackend.model.entity.ArticleLike;
import com.blog.personalblogbackend.notification.NotificationProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InteractionPersistHandler {

    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleFavoriteMapper articleFavoriteMapper;
    private final ArticleMapper articleMapper;
    private final NotificationProducer notificationProducer;
    private final InteractionRedisStore interactionRedisStore;

    @Transactional
    public void handle(InteractionPersistMessage message) {
        if (message == null || message.getAction() == null) {
            return;
        }
        if (InteractionPersistMessage.LIKE_TOGGLE.equals(message.getAction())) {
            persistLikeToggle(message);
        } else if (InteractionPersistMessage.FAVORITE_TOGGLE.equals(message.getAction())) {
            persistFavoriteToggle(message);
        }
    }

    private void persistLikeToggle(InteractionPersistMessage message) {
        Long userId = message.getUserId();
        Long articleId = message.getArticleId();
        Boolean liked = message.getLiked();
        if (userId == null || articleId == null || liked == null) {
            return;
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return;
        }
        boolean exists = articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getArticleId, articleId)) > 0;
        if (liked) {
            if (!exists) {
                ArticleLike like = new ArticleLike();
                like.setUserId(userId);
                like.setArticleId(articleId);
                like.setCreateTime(LocalDateTime.now());
                articleLikeMapper.insert(like);
                notificationProducer.notifyLike(userId, article);
            }
        } else if (exists) {
            articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getUserId, userId)
                    .eq(ArticleLike::getArticleId, articleId));
        }
        interactionRedisStore.setLiked(userId, articleId, liked);
        Integer cached = interactionRedisStore.getLikeCountCached(articleId);
        if (cached == null) {
            Article fresh = articleMapper.selectById(articleId);
            if (fresh != null) {
                interactionRedisStore.seedLikeCount(articleId,
                        fresh.getLikeCount() != null ? fresh.getLikeCount() : 0);
            }
        }
    }

    private void persistFavoriteToggle(InteractionPersistMessage message) {
        Long userId = message.getUserId();
        Long articleId = message.getArticleId();
        Boolean favorited = message.getFavorited();
        if (userId == null || articleId == null || favorited == null) {
            return;
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return;
        }
        boolean exists = articleFavoriteMapper.selectCount(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getUserId, userId)
                .eq(ArticleFavorite::getArticleId, articleId)) > 0;
        if (favorited) {
            if (!exists) {
                ArticleFavorite fav = new ArticleFavorite();
                fav.setUserId(userId);
                fav.setArticleId(articleId);
                fav.setCreateTime(LocalDateTime.now());
                articleFavoriteMapper.insert(fav);
                notificationProducer.notifyFavorite(userId, article);
            }
        } else if (exists) {
            articleFavoriteMapper.delete(new LambdaQueryWrapper<ArticleFavorite>()
                    .eq(ArticleFavorite::getUserId, userId)
                    .eq(ArticleFavorite::getArticleId, articleId));
        }
        interactionRedisStore.setFavorited(userId, articleId, favorited);
    }
}
