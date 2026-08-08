package com.blog.ai.rag.generate;

import com.blog.ai.gateway.model.GatewayResult;
import com.blog.ai.llm.AiService;
import com.blog.ai.rag.model.RetrievedChunk;
import com.blog.ai.rag.retrieve.HybridRetrievalService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagGenerationServiceTest {

    @Test
    void noneEvidenceRefusesWithoutCallingLanguageModel() {
        HybridRetrievalService retrievalService = mock(HybridRetrievalService.class);
        AiService aiService = mock(AiService.class);
        when(retrievalService.retrieve("missing")).thenReturn(result(
                List.of(), HybridRetrievalService.EvidenceLevel.NONE, false, "EMPTY", false));

        var answer = new RagGenerationService(retrievalService, aiService)
                .answer("missing", null, null);

        assertThat(answer.isGrounded()).isFalse();
        assertThat(answer.getConfidence()).isZero();
        assertThat(answer.getRefusalReason()).isEqualTo("NO_RELEVANT_EVIDENCE");
        assertThat(answer.getSources()).isEmpty();
        verify(aiService, never()).chatResult(any(), anyString(), anyString());
    }

    @Test
    void mediumEvidenceCallsLanguageModelAndCapsConfidence() {
        HybridRetrievalService retrievalService = mock(HybridRetrievalService.class);
        AiService aiService = mock(AiService.class);
        RetrievedChunk chunk = new RetrievedChunk("c1", 1L, "doc", 0, "evidence", 1, 0.2);
        when(retrievalService.retrieve("answerable")).thenReturn(result(
                List.of(chunk), HybridRetrievalService.EvidenceLevel.MEDIUM, false, null, false));
        GatewayResult gatewayResult = new GatewayResult();
        gatewayResult.setContent("grounded answer [1]");
        gatewayResult.setOutputTokens(12);
        when(aiService.chatResult(any(), anyString(), anyString())).thenReturn(gatewayResult);

        var answer = new RagGenerationService(retrievalService, aiService)
                .answer("answerable", null, null);

        assertThat(answer.isGrounded()).isTrue();
        assertThat(answer.getConfidence()).isEqualTo(0.60);
        assertThat(answer.getSources()).hasSize(1);
        verify(aiService).chatResult(any(), anyString(), anyString());
    }

    private static HybridRetrievalService.RetrievalResult result(
            List<RetrievedChunk> best,
            HybridRetrievalService.EvidenceLevel level,
            boolean degraded,
            String reason,
            boolean blockedByThreshold) {
        return new HybridRetrievalService.RetrievalResult(
                List.of(), List.of(), best, best, best, level, degraded, reason, blockedByThreshold);
    }
}
