package com.blog.content.search;

import com.blog.content.config.properties.SearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class ElasticsearchIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexInitializer.class);
    private final ObjectProvider<ElasticsearchIndexClient> clientProvider;
    private final ObjectProvider<SearchReindexService> reindexServiceProvider;
    private final SearchProperties searchProperties;

    public ElasticsearchIndexInitializer(ObjectProvider<ElasticsearchIndexClient> clientProvider,
                                           ObjectProvider<SearchReindexService> reindexServiceProvider,
                                           SearchProperties searchProperties) {
        this.clientProvider = clientProvider;
        this.reindexServiceProvider = reindexServiceProvider;
        this.searchProperties = searchProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        ElasticsearchIndexClient client = clientProvider.getIfAvailable();
        if (client == null || !client.isAvailable()) {
            log.warn("[search] Elasticsearch unavailable at {}", searchProperties.getHost());
            return;
        }
        client.ensureIndex();
        client.updateSettings();
        long docs = client.getNumberOfDocuments();
        log.info("[search] Elasticsearch index ready, docs={}", docs);
        if (searchProperties.isInitReindexOnStartup() && docs == 0) {
            SearchReindexService reindexService = reindexServiceProvider.getIfAvailable();
            if (reindexService != null) {
                try {
                    SearchReindexResult result = reindexService.reindexAllPublishedOnStartup();
                    log.info("[search] startup reindex done, indexed={}", result.getIndexed());
                } catch (Exception ex) {
                    log.error("[search] startup reindex failed", ex);
                }
            }
        }
    }
}
