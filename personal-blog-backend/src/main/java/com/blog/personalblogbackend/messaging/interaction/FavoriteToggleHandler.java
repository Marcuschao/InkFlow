package com.blog.personalblogbackend.messaging.interaction;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.interaction.InteractionRedisStore;
import com.blog.personalblogbackend.mapper.ArticleFavoriteMapper;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.messaging.InteractionPersistMessage;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.ArticleFavorite;
import com.blog.personalblogbackend.notification.NotificationProducer;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class FavoriteToggleHandler implements InteractionActionHandler {

    private final ArticleFavoriteMapper articleFavoriteMapper;
    private final ArticleMapper articleMapper;
    private final NotificationProducer notificationProducer;
    private final InteractionRedisStore interactionRedisStore;

    public FavoriteToggleHandler(ArticleFavoriteMapper articleFavoriteMapper,
                                 ArticleMapper articleMapper,
                                 NotificationProducer notificationProducer,
                                 InteractionRedisStore interactionRedisStore) {
        this.articleFavoriteMapper = articleFavoriteMapper;
        this.articleMapper = articleMapper;
        this.notificationProducer = notificationProducer;
        this.interactionRedisStore = interactionRedisStore;
    }

    @Override
    public String action() {
        return InteractionPersistMessage.FAVORITE_TOGGLE;
    }

    @Override
    public void handle(InteractionPersistMessage message) {
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
