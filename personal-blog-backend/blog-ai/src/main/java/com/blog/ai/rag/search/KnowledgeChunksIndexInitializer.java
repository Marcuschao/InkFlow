package com.blog.ai.rag.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.blog.ai.config.properties.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.StringReader;

@Component
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class KnowledgeChunksIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeChunksIndexInitializer.class);

    private final ElasticsearchClient client;
    private final RagProperties properties;

    public KnowledgeChunksIndexInitializer(ElasticsearchClient client, RagProperties properties) {
        this.client = client;
        this.properties = properties;
        ensureIndex();
        long count = countChunks();
        log.info("[rag] index {} ready, chunks={}", properties.getEs().getChunksIndex(), count);
    }

    private long countChunks() {
        try {
            return client.count(c -> c.index(properties.getEs().getChunksIndex())).count();
        } catch (Exception e) {
            log.warn("[rag] count chunks failed: {}", e.getMessage());
            return -1;
        }
    }

    public void ensureIndex() {
        String index = properties.getEs().getChunksIndex();
        int dims = properties.getEs().getVectorDims();
        try {
            boolean exists = client.indices().exists(ExistsRequest.of(e -> e.index(index))).value();
            if (!exists) {
                String mapping = """
                        {
                          "mappings": {
                            "properties": {
                              "chunkId": {"type": "keyword"},
                              "docId": {"type": "long"},
                              "docTitle": {"type": "text"},
                              "ordinal": {"type": "integer"},
                              "text": {"type": "text"},
                              "embedding": {"type": "dense_vector", "dims": %d, "index": true, "similarity": "cosine"},
                              "metadata": {"type": "object", "enabled": false}
                            }
                          }
                        }""".formatted(dims);
                client.indices().create(CreateIndexRequest.of(c -> c.index(index).withJson(new StringReader(mapping))));
                log.info("[rag] created knowledge_chunks index {} dims={}", index, dims);
            }
        } catch (Exception ex) {
            log.error("[rag] ensureIndex failed index={}: {}", index, ex.getMessage());
        }
    }
}
