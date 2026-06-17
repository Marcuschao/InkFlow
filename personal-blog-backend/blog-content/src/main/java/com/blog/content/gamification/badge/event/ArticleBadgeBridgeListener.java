package com.blog.content.gamification.badge.event;

import com.blog.content.event.ArticleLifecycleEvent;
import com.blog.content.gamification.event.ActivityType;
import com.blog.content.gamification.event.UserActivityEventPublisher;
import com.blog.content.model.entity.Article;
import com.blog.content.model.enums.ArticleStatus;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ArticleBadgeBridgeListener {

    private final UserActivityEventPublisher activityEventPublisher;

    public ArticleBadgeBridgeListener(UserActivityEventPublisher activityEventPublisher) {
        this.activityEventPublisher = activityEventPublisher;
    }

    @EventListener
    public void onArticleLifecycle(ArticleLifecycleEvent event) {
        Article previous = event.getPrevious();
        Article fresh = event.getFresh();
        if (fresh == null || !ArticleStatus.isPublished(fresh.getStatus())) {
            return;
        }
        if (previous == null || !ArticleStatus.isPublished(previous.getStatus())) {
            Long authorId = fresh.getAuthorId();
            if (authorId != null) {
                activityEventPublisher.publish(ActivityType.ARTICLE_PUBLISHED, authorId);
            }
        }
    }
}
