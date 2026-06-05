package com.blog.personalblogbackend.search;

import com.blog.personalblogbackend.config.properties.SearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class MeilisearchIndexInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MeilisearchIndexInitializer.class);

    private final MeilisearchClient meilisearchClient;
    private final SearchProperties searchProperties;
    private final SearchReindexService searchReindexService;

    public MeilisearchIndexInitializer(MeilisearchClient meilisearchClient,
                                       SearchProperties searchProperties,
                                       SearchReindexService searchReindexService) {
        this.meilisearchClient = meilisearchClient;
        this.searchProperties = searchProperties;
        this.searchReindexService = searchReindexService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!meilisearchClient.isAvailable()) {
                log.warn("[meili] unavailable at {}, search will fallback to MySQL", searchProperties.getHost());
                return;
            }
            meilisearchClient.ensureIndex();
            meilisearchClient.updateSettings();
            long docs = meilisearchClient.getNumberOfDocuments();
            if (docs == 0) {
                SearchReindexResult result = searchReindexService.reindexAllPublished();
                log.info("[meili] index {} empty, swap-reindexed {} articles via {}",
                        searchProperties.getIndex(), result.getIndexed(), result.getRebuildIndex());
            } else {
                log.info("[meili] index {} initialized ({} documents)", searchProperties.getIndex(), docs);
            }
        } catch (Exception ex) {
            log.warn("[meili] index init failed: {}", ex.toString());
        }
    }
}
