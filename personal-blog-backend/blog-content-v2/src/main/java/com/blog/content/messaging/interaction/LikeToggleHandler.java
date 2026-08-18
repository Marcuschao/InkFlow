package com.blog.content.messaging.interaction;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.interaction.InteractionRedisStore;
import com.blog.content.mapper.ArticleLikeMapper;
import com.blog.content.mapper.ArticleMapper;
import com.blog.content.messaging.InteractionPersistMessage;
import com.blog.content.model.entity.Article;
import com.blog.content.model.entity.ArticleLike;
import com.blog.content.notification.NotificationProducer;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LikeToggleHandler implements InteractionActionHandler {

    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleMapper articleMapper;
    private final NotificationProducer notificationProducer;
    private final InteractionRedisStore interactionRedisStore;

    public LikeToggleHandler(ArticleLikeMapper articleLikeMapper,
                             ArticleMapper articleMapper,
                             NotificationProducer notificationProducer,
                             InteractionRedisStore interactionRedisStore) {
        this.articleLikeMapper = articleLikeMapper;
        this.articleMapper = articleMapper;
        this.notificationProducer = notificationProducer;
        this.interactionRedisStore = interactionRedisStore;
    }

    @Override
    public String action() {
        return InteractionPersistMessage.LIKE_TOGGLE;
    }

    @Override
    public void handle(InteractionPersistMessage message) {
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
}
