package com.blog.personalblogbackend.search;

import com.blog.personalblogbackend.config.properties.SearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class MeilisearchIndexInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MeilisearchIndexInitializer.class);

    private final ObjectProvider<MeilisearchClient> meilisearchClientProvider;
    private final SearchProperties searchProperties;
    private final ObjectProvider<SearchReindexService> searchReindexServiceProvider;

    public MeilisearchIndexInitializer(ObjectProvider<MeilisearchClient> meilisearchClientProvider,
                                       SearchProperties searchProperties,
                                       ObjectProvider<SearchReindexService> searchReindexServiceProvider) {
        this.meilisearchClientProvider = meilisearchClientProvider;
        this.searchProperties = searchProperties;
        this.searchReindexServiceProvider = searchReindexServiceProvider;
    }

    @Override
    public void run(ApplicationArguments args) {
        CompletableFuture.runAsync(this::initialize);
    }

    private void initialize() {
        try {
            MeilisearchClient meilisearchClient = meilisearchClientProvider.getIfAvailable();
            if (meilisearchClient == null) {
                return;
            }
            if (!meilisearchClient.isAvailable()) {
                log.warn("[meili] unavailable at {}, search will fallback to MySQL", searchProperties.getHost());
                return;
            }
            meilisearchClient.ensureIndex();
            meilisearchClient.updateSettings();
            long docs = meilisearchClient.getNumberOfDocuments();
            if (docs == 0 && searchProperties.isInitReindexOnStartup()) {
                SearchReindexService searchReindexService = searchReindexServiceProvider.getIfAvailable();
                if (searchReindexService == null) {
                    return;
                }
                SearchReindexResult result = searchReindexService.reindexAllPublished();
                log.info("[meili] index {} empty, swap-reindexed {} articles via {}",
                        searchProperties.getIndex(), result.getIndexed(), result.getRebuildIndex());
            } else if (docs == 0) {
                log.warn("[meili] index {} empty, skip startup reindex (blog.search.init-reindex-on-startup=false)",
                        searchProperties.getIndex());
            } else {
                log.info("[meili] index {} initialized ({} documents)", searchProperties.getIndex(), docs);
            }
        } catch (Exception ex) {
            log.warn("[meili] index init failed: {}", ex.toString());
        }
    }
}
