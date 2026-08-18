package com.blog.ai.rag.retrieve;

import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.config.properties.RagProperties;
import com.blog.ai.rag.embed.EmbeddingService;
import com.blog.ai.rag.model.RetrievedChunk;
import com.blog.ai.rag.search.KnowledgeChunkIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class HybridRetrievalService {
    private final KnowledgeChunkIndexService indexService;
    private final EmbeddingService embeddingService;
    private final RerankService rerankService;
    private final RagProperties properties;

    public RetrievalResult retrieve(String query) {
        return retrieve(query, RetrievalFilter.publicOnly());
    }

    public RetrievalResult retrieve(String query, RetrievalFilter filter) {
        RagProperties.Retrieve cfg = properties.getRetrieve();
        List<RetrievedChunk> keywordHits = List.of();
        List<RetrievedChunk> vectorHits = List.of();
        boolean keywordFailed = false;
        boolean vectorFailed = false;
        try {
            keywordHits = indexService.keywordSearch(query, cfg.getKeywordTopK());
        } catch (Exception e) {
            keywordFailed = true;
            log.warn("[rag] keyword retrieval failed: {}", e.getMessage());
        }
        try {
            vectorHits = indexService.vectorSearch(embeddingService.embedOne(query), cfg.getVectorTopK());
        } catch (Exception e) {
            vectorFailed = true;
            log.warn("[rag] vector retrieval failed: {}", e.getMessage());
        }
        if (keywordFailed && vectorFailed) {
            throw new ServiceException(503, "知识库检索服务暂时不可用");
        }

        keywordHits = filter.apply(keywordHits);
        vectorHits = filter.apply(vectorHits);
        List<RetrievedChunk> fused = reciprocalRankFusion(keywordHits, vectorHits, cfg.getRrfK(), cfg.getRrfTopK());
        if (fused.isEmpty()) {
            return new RetrievalResult(keywordHits, vectorHits, fused, List.of(), List.of(),
                    EvidenceLevel.NONE, keywordFailed || vectorFailed, "EMPTY", false);
        }

        RerankService.RerankResult rerank = rerankService.rerankWithStatus(query, fused, cfg.getFinalTopK());
        List<RetrievedChunk> best = rerank.chunks().isEmpty() ? fused : rerank.chunks();
        boolean degraded = keywordFailed || vectorFailed || rerank.degraded();
        String reason = keywordFailed ? "KEYWORD_UNAVAILABLE"
                : vectorFailed ? "EMBEDDING_UNAVAILABLE" : rerank.reason();
        EvidenceLevel level = classify(best, rerank.applied(), cfg);
        boolean blockedByThreshold = level == EvidenceLevel.NONE && !best.isEmpty();
        return new RetrievalResult(keywordHits, vectorHits, fused, rerank.chunks(), best,
                level, degraded, reason, blockedByThreshold, RetrievalAudit.from(keywordHits, vectorHits, fused, rerank.chunks(), best));
    }

    private EvidenceLevel classify(List<RetrievedChunk> best, boolean rerankApplied, RagProperties.Retrieve cfg) {
        if (best == null || best.isEmpty()) return EvidenceLevel.NONE;
        if (!rerankApplied) return best.size() == 1 ? EvidenceLevel.LOW : EvidenceLevel.MEDIUM;
        double threshold = cfg.getCalibratedRerankThreshold();
        double topScore = best.stream().mapToDouble(RetrievedChunk::getRerankScore).max().orElse(0);
        if (threshold < 0) return EvidenceLevel.MEDIUM;
        if (topScore < threshold) {
            return "ENFORCE".equalsIgnoreCase(cfg.getEvidenceGateMode()) ? EvidenceLevel.NONE : EvidenceLevel.LOW;
        }
        return EvidenceLevel.HIGH;
    }

    private List<RetrievedChunk> reciprocalRankFusion(List<RetrievedChunk> a, List<RetrievedChunk> b, int k, int topK) {
        Map<String, RetrievedChunk> byId = new HashMap<>();
        Map<String, Double> scores = new HashMap<>();
        accumulate(a, k, byId, scores);
        accumulate(b, k, byId, scores);
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((x, y) -> Double.compare(y.getValue(), x.getValue()));
        List<RetrievedChunk> out = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, sorted.size()); i++) {
            RetrievedChunk source = byId.get(sorted.get(i).getKey());
            if (source != null) {
                RetrievedChunk copy = new RetrievedChunk();
                copy.setChunkId(source.getChunkId()); copy.setDocId(source.getDocId()); copy.setDocTitle(source.getDocTitle());
                copy.setOrdinal(source.getOrdinal()); copy.setText(source.getText()); copy.setScore(source.getScore());
                copy.setRerankScore(sorted.get(i).getValue()); copy.setTenantId(source.getTenantId());
                copy.setWorkspaceId(source.getWorkspaceId()); copy.setOwnerId(source.getOwnerId());
                copy.setVisibility(source.getVisibility()); copy.setDocumentVersion(source.getDocumentVersion());
                out.add(copy);
            }
        }
        return out;
    }

    private void accumulate(List<RetrievedChunk> hits, int k, Map<String, RetrievedChunk> byId, Map<String, Double> scores) {
        if (hits == null) return;
        for (int rank = 0; rank < hits.size(); rank++) {
            RetrievedChunk chunk = hits.get(rank);
            byId.putIfAbsent(chunk.getChunkId(), chunk);
            scores.merge(chunk.getChunkId(), 1.0 / (k + rank + 1), Double::sum);
        }
    }

    public enum EvidenceLevel { HIGH, MEDIUM, LOW, NONE }

    public static class RetrievalResult {
        public final List<RetrievedChunk> keywordHits;
        public final List<RetrievedChunk> vectorHits;
        public final List<RetrievedChunk> fused;
        public final List<RetrievedChunk> reranked;
        public final List<RetrievedChunk> best;
        public final EvidenceLevel evidenceLevel;
        public final boolean degraded;
        public final String reason;
        public final boolean blockedByThreshold;

        public RetrievalResult(List<RetrievedChunk> keywordHits, List<RetrievedChunk> vectorHits,
                               List<RetrievedChunk> fused, List<RetrievedChunk> reranked,
                               List<RetrievedChunk> best, EvidenceLevel evidenceLevel,
                               boolean degraded, String reason, boolean blockedByThreshold, RetrievalAudit audit) {
            this.keywordHits = keywordHits;
            this.vectorHits = vectorHits;
            this.fused = fused;
            this.reranked = reranked;
            this.best = best;
            this.evidenceLevel = evidenceLevel;
            this.degraded = degraded;
            this.reason = reason;
            this.blockedByThreshold = blockedByThreshold;
            this.audit = audit;
        }
        public RetrievalResult(List<RetrievedChunk> keywordHits, List<RetrievedChunk> vectorHits,
                               List<RetrievedChunk> fused, List<RetrievedChunk> reranked,
                               List<RetrievedChunk> best, EvidenceLevel evidenceLevel,
                               boolean degraded, String reason, boolean blockedByThreshold) {
            this(keywordHits, vectorHits, fused, reranked, best, evidenceLevel, degraded, reason,
                    blockedByThreshold, RetrievalAudit.from(keywordHits, vectorHits, fused, reranked, best));
        }
        public final RetrievalAudit audit;
    }

    public record RetrievalFilter(String tenantId, String workspaceId, Long ownerId, boolean includePublic) {
        public static RetrievalFilter publicOnly() { return new RetrievalFilter(null, null, null, true); }
        public List<RetrievedChunk> apply(List<RetrievedChunk> chunks) {
            if (chunks == null || chunks.isEmpty()) return List.of();
            return chunks.stream().filter(c -> {
                if (includePublic && (c.getVisibility() == null || "PUBLIC".equalsIgnoreCase(c.getVisibility()))) return true;
                return (tenantId != null && tenantId.equals(c.getTenantId()))
                        && (workspaceId == null || workspaceId.equals(c.getWorkspaceId()))
                        && (ownerId == null || ownerId.equals(c.getOwnerId()));
            }).toList();
        }
    }

    public record RetrievalAudit(List<AuditHit> keyword, List<AuditHit> vector, List<AuditHit> fused,
                                 List<AuditHit> reranked, List<AuditHit> adopted) {
        static RetrievalAudit from(List<RetrievedChunk> k, List<RetrievedChunk> v, List<RetrievedChunk> f,
                                   List<RetrievedChunk> r, List<RetrievedChunk> a) {
            return new RetrievalAudit(toHits(k), toHits(v), toHits(f), toHits(r), toHits(a));
        }
        private static List<AuditHit> toHits(List<RetrievedChunk> list) {
            return list == null ? List.of() : list.stream().map(c -> new AuditHit(c.getChunkId(), c.getDocId(), c.getScore(), c.getRerankScore())).toList();
        }
    }
    public record AuditHit(String chunkId, Long docId, double score, double rerankScore) {}
}
