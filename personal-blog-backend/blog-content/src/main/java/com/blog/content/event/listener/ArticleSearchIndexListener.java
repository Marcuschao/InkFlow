package com.blog.content.event.listener;

import com.blog.content.event.ArticleLifecycleEvent;
import com.blog.content.messaging.SearchSyncEventType;
import com.blog.content.model.entity.Article;
import com.blog.content.model.enums.ArticleStatus;
import com.blog.content.search.SearchOutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 设计模式：观察者模式 - 响应文章生命周期事件并同步搜索索引
@Component
public class ArticleSearchIndexListener {

    @Autowired(required = false)
    private SearchOutboxService searchOutboxService;

    @EventListener
    public void onArticleLifecycle(ArticleLifecycleEvent event) {
        if (searchOutboxService == null) {
            return;
        }
        Article previous = event.getPrevious();
        Article fresh = event.getFresh();
        if (fresh != null && ArticleStatus.isPublished(fresh.getStatus())) {
            searchOutboxService.enqueue(fresh.getId(), SearchSyncEventType.UPSERT, fresh.getUpdateTime());
        } else if (previous != null && ArticleStatus.isPublished(previous.getStatus())) {
            LocalDateTime at = previous.getUpdateTime() != null ? previous.getUpdateTime() : LocalDateTime.now();
            searchOutboxService.enqueue(previous.getId(), SearchSyncEventType.DELETE, at);
        }
    }
}
