package com.blog.content.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.blog.content.config.properties.SearchProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class ElasticsearchIndexClient {

    private final ElasticsearchClient client;
    private final SearchProperties properties;
    private final ObjectMapper objectMapper;

    public ElasticsearchIndexClient(ElasticsearchClient client, SearchProperties properties, ObjectMapper objectMapper) {
        this.client = client;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean isAvailable() {
        try {
            return client.ping().value();
        } catch (Exception ex) {
            return false;
        }
    }

    public void ensureIndex() {
        ensureIndex(properties.getIndex());
    }

    public void ensureIndex(String index) {
        try {
            boolean exists = client.indices().exists(ExistsRequest.of(e -> e.index(index))).value();
            if (!exists) {
                client.indices().create(CreateIndexRequest.of(c -> c.index(index)
                        .withJson(new StringReader("""
                                {"mappings":{"properties":{
                                "id":{"type":"long"},"title":{"type":"text"},"summary":{"type":"text"},
                                "content":{"type":"text"},"cover":{"type":"keyword"},
                                "categoryId":{"type":"long"},"authorId":{"type":"long"},
                                "status":{"type":"integer"},"viewCount":{"type":"integer"},
                                "createTime":{"type":"date"},"updateTime":{"type":"date"}
                                }}}"""))));
            }
        } catch (Exception ignored) {
        }
    }

    public void updateSettings() {
        ensureIndex();
    }

    public void updateSettings(String indexUid) {
        ensureIndex(indexUid);
    }

    public void addOrReplaceDocuments(List<ArticleSearchDocument> documents) {
        addOrReplaceDocuments(properties.getIndex(), documents);
    }

    public void addOrReplaceDocuments(String indexUid, List<ArticleSearchDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        try {
            BulkRequest.Builder bulk = new BulkRequest.Builder();
            for (ArticleSearchDocument doc : documents) {
                bulk.operations(BulkOperation.of(op -> op.index(IndexOperation.of(i -> i
                        .index(indexUid).id(String.valueOf(doc.getId())).document(doc)))));
            }
            client.bulk(bulk.build());
        } catch (Exception ignored) {
        }
    }

    public void deleteDocument(Long id) {
        if (id == null) {
            return;
        }
        try {
            client.delete(d -> d.index(properties.getIndex()).id(String.valueOf(id)));
        } catch (Exception ignored) {
        }
    }

    public long getNumberOfDocuments() {
        return getNumberOfDocuments(properties.getIndex());
    }

    public long getNumberOfDocuments(String indexUid) {
        try {
            return client.count(c -> c.index(indexUid)).count();
        } catch (Exception ex) {
            return 0;
        }
    }

    public Set<Long> fetchExistingIds(String indexUid, List<Long> ids) {
        Set<Long> found = new HashSet<>();
        if (ids == null || ids.isEmpty()) {
            return found;
        }
        try {
            SearchResponse<ArticleSearchDocument> resp = client.search(SearchRequest.of(s -> s
                    .index(indexUid).size(ids.size())
                    .query(Query.of(q -> q.ids(i -> i.values(ids.stream().map(String::valueOf).toList()))))),
                    ArticleSearchDocument.class);
            resp.hits().hits().forEach(h -> {
                if (h.source() != null && h.source().getId() != null) {
                    found.add(h.source().getId());
                }
            });
        } catch (Exception ignored) {
        }
        return found;
    }

    public void swapIndexes(String uidA, String uidB) {
    }

    public void deleteIndex(String indexUid) {
        try {
            client.indices().delete(d -> d.index(indexUid));
        } catch (Exception ignored) {
        }
    }

    public JsonNode search(String query, int limit, int offset) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode hits = objectMapper.createArrayNode();
        try {
            SearchResponse<ArticleSearchDocument> resp = client.search(SearchRequest.of(s -> s
                    .index(properties.getIndex()).from(offset).size(limit)
                    .query(Query.of(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("status").value(FieldValue.of(1))))
                            .must(m -> m.multiMatch(mm -> mm.fields("title", "summary", "content")
                                    .query(query != null ? query : ""))))))),
                    ArticleSearchDocument.class);
            long total = resp.hits().total() != null ? resp.hits().total().value() : 0;
            root.put("estimatedTotalHits", total);
            resp.hits().hits().forEach(h -> {
                if (h.source() == null) {
                    return;
                }
                ObjectNode hit = objectMapper.valueToTree(h.source());
                hits.add(hit);
            });
        } catch (Exception ignored) {
        }
        root.set("hits", hits);
        return root;
    }
}
