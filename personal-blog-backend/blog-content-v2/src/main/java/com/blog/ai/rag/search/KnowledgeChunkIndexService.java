package com.blog.ai.rag.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.config.properties.RagProperties;
import com.blog.ai.rag.model.KnowledgeChunkDoc;
import com.blog.ai.rag.model.RetrievedChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class KnowledgeChunkIndexService {

    private static final List<String> SOURCE_FIELDS = List.of("chunkId", "docId", "docTitle", "ordinal", "text",
            "tenantId", "workspaceId", "ownerId", "visibility", "documentVersion");

    private final ElasticsearchClient client;
    private final RagProperties properties;

    public KnowledgeChunkIndexService(
            @Qualifier("ragElasticsearchClient") ElasticsearchClient client,
            RagProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public void indexChunks(List<KnowledgeChunkDoc> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new ServiceException(400, "无可索引的分块");
        }
        String index = properties.getEs().getChunksIndex();
        try {
            BulkRequest.Builder bulk = new BulkRequest.Builder().refresh(Refresh.True);
            for (KnowledgeChunkDoc doc : chunks) {
                bulk.operations(BulkOperation.of(op -> op.index(IndexOperation.of(i -> i
                        .index(index).id(doc.getChunkId()).document(doc)))));
            }
            var resp = client.bulk(bulk.build());
            if (resp.errors()) {
                String reason = resp.items().stream()
                        .filter(item -> item.error() != null)
                        .map(item -> item.id() + ": " + item.error().reason())
                        .findFirst()
                        .orElse("unknown bulk error");
                throw new ServiceException(500, "ES 索引失败: " + reason);
            }
            log.info("[rag] indexed {} chunks into {}", chunks.size(), index);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(500, "ES 索引失败: " + e.getMessage());
        }
    }

    public long countAll() {
        String index = properties.getEs().getChunksIndex();
        try {
            return client.count(c -> c.index(index)).count();
        } catch (Exception e) {
            log.warn("[rag] countAll failed: {}", e.getMessage());
            return -1;
        }
    }

    public void deleteByDocId(Long docId) {
        if (docId == null) {
            return;
        }
        String index = properties.getEs().getChunksIndex();
        try {
            client.deleteByQuery(d -> d.index(index).refresh(true)
                    .query(Query.of(q -> q.term(t -> t.field("docId").value(FieldValue.of(docId))))));
        } catch (Exception e) {
            log.warn("[rag] deleteByDocId failed docId={}: {}", docId, e.getMessage());
        }
    }

    public List<RetrievedChunk> keywordSearch(String query, int topK) {
        String index = properties.getEs().getChunksIndex();
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        List<RetrievedChunk> out = new ArrayList<>();
        try {
            String normalized = normalizeQuery(query);
            SearchResponse<KnowledgeChunkDoc> resp = client.search(SearchRequest.of(s -> s
                    .index(index)
                    .size(topK)
                    .source(src -> src.filter(f -> f.includes(SOURCE_FIELDS)))
                    .query(Query.of(q -> q.multiMatch(mm -> mm
                            .fields("text", "docTitle")
                            .query(normalized)
                            .type(TextQueryType.BestFields)
                            .operator(Operator.Or)
                            .minimumShouldMatch("1"))))
            ), KnowledgeChunkDoc.class);
            collectHits(resp, out);
            if (out.isEmpty() && !normalized.equals(query.trim())) {
                SearchResponse<KnowledgeChunkDoc> retry = client.search(SearchRequest.of(s -> s
                        .index(index)
                        .size(topK)
                        .source(src -> src.filter(f -> f.includes(SOURCE_FIELDS)))
                        .query(Query.of(q -> q.multiMatch(mm -> mm
                                .fields("text", "docTitle")
                                .query(query.trim())
                                .type(TextQueryType.BestFields)
                                .operator(Operator.Or)
                                .minimumShouldMatch("1"))))
                ), KnowledgeChunkDoc.class);
                collectHits(retry, out);
            }
        } catch (Exception e) {
            log.warn("[rag] keywordSearch failed: {}", e.getMessage());
        }
        return out;
    }

    public List<RetrievedChunk> vectorSearch(float[] vector, int topK) {
        String index = properties.getEs().getChunksIndex();
        List<RetrievedChunk> out = new ArrayList<>();
        if (vector == null || vector.length == 0) {
            return out;
        }
        int expected = properties.getEs().getVectorDims();
        if (vector.length != expected) {
            log.warn("[rag] vectorSearch skipped: query dims {} != config {}", vector.length, expected);
            return out;
        }
        try {
            int candidates = Math.max(topK * 5, 50);
            SearchResponse<KnowledgeChunkDoc> resp = client.search(SearchRequest.of(s -> s
                    .index(index)
                    .size(topK)
                    .source(src -> src.filter(f -> f.includes(SOURCE_FIELDS)))
                    .knn(k -> k
                            .field("embedding")
                            .queryVector(floatToList(vector))
                            .k(topK)
                            .numCandidates(candidates))
            ), KnowledgeChunkDoc.class);
            collectHits(resp, out);
        } catch (Exception e) {
            log.warn("[rag] vectorSearch failed: {}", e.getMessage());
        }
        return out;
    }

    private void collectHits(SearchResponse<KnowledgeChunkDoc> resp, List<RetrievedChunk> out) {
        for (Hit<KnowledgeChunkDoc> hit : resp.hits().hits()) {
            KnowledgeChunkDoc src = hit.source();
            if (src == null) {
                continue;
            }
            RetrievedChunk rc = toRetrieved(src);
            rc.setScore(hit.score() != null ? hit.score() : 0.0);
            out.add(rc);
        }
    }

    private RetrievedChunk toRetrieved(KnowledgeChunkDoc src) {
        RetrievedChunk rc = new RetrievedChunk();
        rc.setChunkId(src.getChunkId());
        rc.setDocId(src.getDocId());
        rc.setDocTitle(src.getDocTitle());
        rc.setOrdinal(src.getOrdinal());
        rc.setText(src.getText());
        rc.setTenantId(src.getTenantId());
        rc.setWorkspaceId(src.getWorkspaceId());
        rc.setOwnerId(src.getOwnerId());
        rc.setVisibility(src.getVisibility());
        rc.setDocumentVersion(src.getDocumentVersion());
        return rc;
    }

    private static String normalizeQuery(String query) {
        return query.replaceAll("[?？！!。，,；;：:\\s]+", " ").trim();
    }

    private List<Float> floatToList(float[] vec) {
        List<Float> list = new ArrayList<>(vec.length);
        for (float v : vec) {
            list.add(v);
        }
        return list;
    }
}
