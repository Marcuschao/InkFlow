package com.blog.content.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.concurrency.DistributedLockService;
import com.blog.content.config.properties.SearchProperties;
import com.blog.content.mapper.ArticleMapper;
import com.blog.content.model.entity.Article;
import com.blog.content.model.enums.ArticleStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Lazy
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class SearchReindexService {

    private static final int BATCH_SIZE = 200;
    private static final DateTimeFormatter REBUILD_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ArticleMapper articleMapper;
    private final SearchIndexService searchIndexService;
    private final ElasticsearchIndexClient meilisearchClient;
    private final SearchProperties searchProperties;
    private final DistributedLockService distributedLockService;

    public SearchReindexService(ArticleMapper articleMapper,
                                SearchIndexService searchIndexService,
                                ElasticsearchIndexClient meilisearchClient,
                                SearchProperties searchProperties,
                                DistributedLockService distributedLockService) {
        this.articleMapper = articleMapper;
        this.searchIndexService = searchIndexService;
        this.meilisearchClient = meilisearchClient;
        this.searchProperties = searchProperties;
        this.distributedLockService = distributedLockService;
    }

    public SearchReindexResult reindexAllPublished() {
        if (!meilisearchClient.isAvailable()) {
            throw new IllegalStateException("Meilisearch unavailable at " + searchProperties.getHost());
        }
        return distributedLockService.executeWithLock("lock:search:reindex", () -> doSwapReindex());
    }

    private SearchReindexResult doSwapReindex() {
        String primaryIndex = searchProperties.getIndex();
        String rebuildIndex = primaryIndex + "_rebuild_" + LocalDateTime.now().format(REBUILD_SUFFIX);

        meilisearchClient.ensureIndex(primaryIndex);
        meilisearchClient.ensureIndex(rebuildIndex);
        meilisearchClient.updateSettings(rebuildIndex);

        int total = 0;
        long pageNum = 1;
        while (true) {
            Page<Article> page = articleMapper.selectPage(
                    new Page<>(pageNum, BATCH_SIZE),
                    new LambdaQueryWrapper<Article>().eq(Article::getStatus, ArticleStatus.PUBLISHED));
            if (page.getRecords().isEmpty()) {
                break;
            }
            searchIndexService.reindexBatch(rebuildIndex, page.getRecords());
            total += page.getRecords().size();
            if (page.getRecords().size() < BATCH_SIZE) {
                break;
            }
            pageNum++;
        }

        meilisearchClient.swapIndexes(primaryIndex, rebuildIndex);
        meilisearchClient.deleteIndex(rebuildIndex);
        meilisearchClient.updateSettings(primaryIndex);

        return new SearchReindexResult(total, rebuildIndex, true);
    }
}
