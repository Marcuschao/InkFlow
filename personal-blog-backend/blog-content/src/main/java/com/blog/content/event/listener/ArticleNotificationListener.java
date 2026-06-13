package com.blog.content.event.listener;

import com.blog.content.event.ArticleLifecycleEvent;
import com.blog.content.model.entity.Article;
import com.blog.content.model.enums.ArticleStatus;
import com.blog.content.notification.DomainEvent;
import com.blog.content.notification.NotificationProducer;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// 设计模式：观察者模式 - 响应文章生命周期事件并发送通知
@Component
public class ArticleNotificationListener {

    private final NotificationProducer notificationProducer;

    public ArticleNotificationListener(NotificationProducer notificationProducer) {
        this.notificationProducer = notificationProducer;
    }

    @EventListener
    public void onArticleLifecycle(ArticleLifecycleEvent event) {
        Article previous = event.getPrevious();
        Article fresh = event.getFresh();
        if (fresh == null || !ArticleStatus.isPublished(fresh.getStatus())) {
            return;
        }
        if (previous == null || !ArticleStatus.isPublished(previous.getStatus())) {
            notificationProducer.sendArticlePublished(fresh);
            notificationProducer.sendDomainEvent(DomainEvent.articlePublished(fresh));
        } else {
            notificationProducer.sendDomainEvent(DomainEvent.articleUpdated(fresh));
        }
    }
}
