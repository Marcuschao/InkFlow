package com.blog.personalblogbackend.event.listener;

import com.blog.personalblogbackend.event.ArticleLifecycleEvent;
import com.blog.personalblogbackend.messaging.ContentChangeEventType;
import com.blog.personalblogbackend.messaging.ContentChangeProducer;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.enums.ArticleStatus;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// 设计模式：观察者模式 - 响应文章生命周期事件并失效缓存
@Component
public class ArticleContentChangeListener {

    private final ContentChangeProducer contentChangeProducer;

    public ArticleContentChangeListener(ContentChangeProducer contentChangeProducer) {
        this.contentChangeProducer = contentChangeProducer;
    }

    @EventListener
    public void onArticleLifecycle(ArticleLifecycleEvent event) {
        Article fresh = event.getFresh();
        if (fresh == null || !ArticleStatus.isPublished(fresh.getStatus())) {
            return;
        }
        contentChangeProducer.send(fresh.getId(), ContentChangeEventType.ARTICLE_UPDATED);
    }
}
