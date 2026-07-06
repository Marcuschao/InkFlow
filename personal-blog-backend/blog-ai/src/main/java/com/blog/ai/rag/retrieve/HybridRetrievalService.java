package com.blog.ai.rag.retrieve;

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
        RagProperties.Retrieve cfg = properties.getRetrieve();
        List<RetrievedChunk> keywordHits = indexService.keywordSearch(query, cfg.getKeywordTopK());
        float[] queryVec = embeddingService.embedOne(query);
        List<RetrievedChunk> vectorHits = indexService.vectorSearch(queryVec, cfg.getVectorTopK());
        List<RetrievedChunk> fused = reciprocalRankFusion(keywordHits, vectorHits, cfg.getRrfK(), cfg.getRrfTopK());
        List<RetrievedChunk> reranked = rerankService.rerank(query, fused, cfg.getFinalTopK());
        if (keywordHits.isEmpty() && vectorHits.isEmpty()) {
            log.warn("[rag] retrieve empty query={} esChunks={}", query, indexService.countAll());
        }
        return new RetrievalResult(keywordHits, vectorHits, fused, reranked);
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
            String id = sorted.get(i).getKey();
            RetrievedChunk rc = byId.get(id);
            if (rc != null) {
                RetrievedChunk copy = new RetrievedChunk(rc.getChunkId(), rc.getDocId(), rc.getDocTitle(),
                        rc.getOrdinal(), rc.getText(), rc.getScore(), 0.0);
                copy.setRerankScore(sorted.get(i).getValue());
                out.add(copy);
            }
        }
        return out;
    }

    private void accumulate(List<RetrievedChunk> hits, int k, Map<String, RetrievedChunk> byId, Map<String, Double> scores) {
        for (int rank = 0; rank < hits.size(); rank++) {
            RetrievedChunk c = hits.get(rank);
            String id = c.getChunkId();
            byId.putIfAbsent(id, c);
            scores.merge(id, 1.0 / (k + rank + 1), Double::sum);
        }
    }

    public static class RetrievalResult {
        public final List<RetrievedChunk> keywordHits;
        public final List<RetrievedChunk> vectorHits;
        public final List<RetrievedChunk> fused;
        public final List<RetrievedChunk> reranked;

        public RetrievalResult(List<RetrievedChunk> keywordHits, List<RetrievedChunk> vectorHits,
                               List<RetrievedChunk> fused, List<RetrievedChunk> reranked) {
            this.keywordHits = keywordHits;
            this.vectorHits = vectorHits;
            this.fused = fused;
            this.reranked = reranked;
        }
    }
}
