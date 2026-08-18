package com.blog.content.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.config.properties.SearchProperties;
import com.blog.content.mapper.ArticleMapper;
import com.blog.content.messaging.SearchSyncEventType;
import com.blog.content.model.entity.Article;
import com.blog.content.model.enums.ArticleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class SearchReconcileService {
    private static final Logger log = LoggerFactory.getLogger(SearchReconcileService.class);

    private final ArticleMapper articleMapper;
    private final ElasticsearchIndexClient meilisearchClient;
    private final SearchOutboxService searchOutboxService;
    private final SearchProperties searchProperties;

    public SearchReconcileService(ArticleMapper articleMapper,
                                  ElasticsearchIndexClient meilisearchClient,
                                  SearchOutboxService searchOutboxService,
                                  SearchProperties searchProperties) {
        this.articleMapper = articleMapper;
        this.meilisearchClient = meilisearchClient;
        this.searchOutboxService = searchOutboxService;
        this.searchProperties = searchProperties;
    }

    public int reconcile() {
        if (!meilisearchClient.isAvailable()) {
            log.warn("[search-reconcile] Meilisearch unavailable, skipped");
            return 0;
        }
        String index = searchProperties.getIndex();
        meilisearchClient.ensureIndex(index);

        long dbCount = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, ArticleStatus.PUBLISHED));
        long meiliCount = meilisearchClient.getNumberOfDocuments(index);
        log.info("[search-reconcile] dbPublished={} meiliDocs={} diff={}", dbCount, meiliCount, dbCount - meiliCount);

        int batchSize = searchProperties.getReconcile().getBatchSize();
        int enqueued = 0;
        long pageNum = 1;
        while (true) {
            Page<Article> page = articleMapper.selectPage(
                    new Page<>(pageNum, batchSize),
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                            .select(Article::getId, Article::getUpdateTime));
            if (page.getRecords().isEmpty()) {
                break;
            }
            List<Long> ids = new ArrayList<>();
            for (Article a : page.getRecords()) {
                ids.add(a.getId());
            }
            Set<Long> found = meilisearchClient.fetchExistingIds(index, ids);
            for (Article a : page.getRecords()) {
                if (!found.contains(a.getId())) {
                    searchOutboxService.enqueue(a.getId(), SearchSyncEventType.UPSERT, a.getUpdateTime());
                    enqueued++;
                }
            }
            if (page.getRecords().size() < batchSize) {
                break;
            }
            pageNum++;
        }
        if (enqueued > 0) {
            log.info("[search-reconcile] enqueued {} missing documents", enqueued);
        }
        return enqueued;
    }
}
