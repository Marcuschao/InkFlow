package com.blog.content.event.listener;

import com.blog.content.event.ArticleLifecycleEvent;
import com.blog.content.knowledge.cache.KnowledgeGraphCacheService;
import com.blog.content.knowledge.service.KnowledgeSubscriptionService;
import com.blog.content.mapper.ArticleMapper;
import com.blog.content.model.entity.Article;
import com.blog.content.model.enums.ArticleStatus;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArticleKnowledgeListener {

    private final KnowledgeSubscriptionService subscriptionService;
    private final KnowledgeGraphCacheService cacheService;
    private final ArticleMapper articleMapper;

    public ArticleKnowledgeListener(KnowledgeSubscriptionService subscriptionService,
                                      KnowledgeGraphCacheService cacheService,
                                      ArticleMapper articleMapper) {
        this.subscriptionService = subscriptionService;
        this.cacheService = cacheService;
        this.articleMapper = articleMapper;
    }

    @EventListener
    public void onArticleLifecycle(ArticleLifecycleEvent event) {
        Article previous = event.getPrevious();
        Article fresh = event.getFresh();
        if (fresh == null || !ArticleStatus.isPublished(fresh.getStatus())) {
            return;
        }
        if (previous == null || !ArticleStatus.isPublished(previous.getStatus())) {
            List<Long> tagIds = articleMapper.selectTagIdsByArticleId(fresh.getId());
            subscriptionService.notifySubscribers(fresh.getId(), tagIds, fresh.getTitle());
        }
        cacheService.evictGraph();
    }
}
