package com.blog.ai.rag.retrieve;

import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.config.properties.RagProperties;
import com.blog.ai.rag.embed.EmbeddingService;
import com.blog.ai.rag.model.RetrievedChunk;
import com.blog.ai.rag.search.KnowledgeChunkIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HybridRetrievalServiceTest {
    private KnowledgeChunkIndexService index;
    private EmbeddingService embedding;
    private RerankService rerank;
    private HybridRetrievalService service;

    @BeforeEach
    void setUp() {
        index = mock(KnowledgeChunkIndexService.class);
        embedding = mock(EmbeddingService.class);
        rerank = mock(RerankService.class);
        service = new HybridRetrievalService(index, embedding, rerank, new RagProperties());
    }

    @Test
    void emptyRetrievalProducesNone() {
        when(index.keywordSearch(anyString(), anyInt())).thenReturn(List.of());
        when(embedding.embedOne(anyString())).thenReturn(new float[]{1});
        when(index.vectorSearch(any(float[].class), anyInt())).thenReturn(List.of());

        var result = service.retrieve("missing");

        assertThat(result.evidenceLevel).isEqualTo(HybridRetrievalService.EvidenceLevel.NONE);
    }

    @Test
    void vectorFailureFallsBackToKeywordEvidence() {
        RetrievedChunk chunk = new RetrievedChunk("c1", 1L, "doc", 0, "text", 1, 0);
        when(index.keywordSearch(anyString(), anyInt())).thenReturn(List.of(chunk));
        when(embedding.embedOne(anyString())).thenThrow(new IllegalStateException("down"));
        when(rerank.rerankWithStatus(anyString(), anyList(), anyInt()))
                .thenReturn(new RerankService.RerankResult(List.of(chunk), false, true, "RERANK_UNAVAILABLE"));

        var result = service.retrieve("q");

        assertThat(result.evidenceLevel).isEqualTo(HybridRetrievalService.EvidenceLevel.LOW);
        assertThat(result.degraded).isTrue();
        assertThat(result.reason).isEqualTo("EMBEDDING_UNAVAILABLE");
    }

    @Test
    void allRetrieversFailAsServiceDegradation() {
        when(index.keywordSearch(anyString(), anyInt())).thenThrow(new IllegalStateException("down"));
        when(embedding.embedOne(anyString())).thenThrow(new IllegalStateException("down"));

        assertThatThrownBy(() -> service.retrieve("q"))
                .isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(503);
    }
}
