package com.blog.content.event.listener;

import com.blog.content.event.ArticleLifecycleEvent;
import com.blog.content.messaging.ContentChangeEventType;
import com.blog.content.messaging.ContentChangeProducer;
import com.blog.content.model.entity.Article;
import com.blog.content.model.enums.ArticleStatus;
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
