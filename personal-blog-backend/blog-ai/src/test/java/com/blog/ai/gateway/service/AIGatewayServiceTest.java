package com.blog.ai.gateway.service;

import com.blog.ai.gateway.AiTaskType;
import com.blog.ai.gateway.config.GatewayProperties;
import com.blog.ai.gateway.factory.ChatModelFactory;
import com.blog.ai.gateway.factory.GatewayChatModels;
import com.blog.ai.gateway.guard.PromptGuard;
import com.blog.ai.gateway.health.ModelHealthChecker;
import com.blog.ai.gateway.model.ModelStreamChunk;
import com.blog.ai.gateway.model.ModelTarget;
import com.blog.ai.gateway.mq.AiCallLogProducer;
import com.blog.ai.gateway.quota.TokenQuotaService;
import com.blog.ai.gateway.router.ModelRouter;
import com.blog.ai.mapper.AiCallLogMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIGatewayServiceTest {

    private final GatewayProperties properties = new GatewayProperties();
    private final ModelRouter router = mock(ModelRouter.class);
    private final ChatModelFactory factory = mock(ChatModelFactory.class);
    private final ModelHealthChecker health = mock(ModelHealthChecker.class);
    private final TokenQuotaService quota = mock(TokenQuotaService.class);
    private final PromptGuard guard = mock(PromptGuard.class);
    private final AiCallLogProducer logProducer = mock(AiCallLogProducer.class);
    private final AiCallLogMapper logMapper = mock(AiCallLogMapper.class);
    private final ModelTarget target = new ModelTarget("test", "test-model", 5000, 0.001, 0.002);

    @BeforeEach
    void setUp() {
        when(router.resolveChain(any())).thenReturn(List.of(target));
        when(guard.filterOutput(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void mapsLangChainResponseAndTokenUsage() {
        ChatModel chatModel = mock(ChatModel.class);
        StreamingChatModel streamingModel = mock(StreamingChatModel.class);
        when(factory.get(target)).thenReturn(new GatewayChatModels(chatModel, streamingModel));
        when(chatModel.chat(any(ChatRequest.class))).thenReturn(ChatResponse.builder()
                .aiMessage(AiMessage.from("answer"))
                .modelName("test-model")
                .tokenUsage(new TokenUsage(5, 2))
                .build());

        var result = gateway().chat(AiTaskType.RAG, "system", "question");

        assertThat(result.getContent()).isEqualTo("answer");
        assertThat(result.getInputTokens()).isEqualTo(5);
        assertThat(result.getOutputTokens()).isEqualTo(2);
        assertThat(result.getCost()).isEqualTo(0.000009);
        verify(quota).recordUsage(null, 7);
        verify(health).recordSuccess("test");
    }

    @Test
    void streamsPartialTextAndEmitsFinalUsageMetadata() {
        ChatModel chatModel = mock(ChatModel.class);
        StreamingChatModel streamingModel = mock(StreamingChatModel.class);
        when(factory.get(target)).thenReturn(new GatewayChatModels(chatModel, streamingModel));
        doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onPartialResponse("Hel");
            handler.onPartialResponse("lo");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("Hello"))
                    .tokenUsage(new TokenUsage(11, 3))
                    .build());
            return null;
        }).when(streamingModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

        List<ModelStreamChunk> chunks = gateway().stream(AiTaskType.RAG, "system", "question", null, "guest")
                .collectList().block();

        assertThat(chunks).hasSize(3);
        assertChunk(chunks.get(0), "Hel", 0, 0);
        assertChunk(chunks.get(1), "lo", 0, 0);
        assertChunk(chunks.get(2), "", 11, 3);

        verify(quota).recordUsage(null, 14);
        verify(health).recordSuccess("test");
    }

    @Test
    void fallsBackWhenPrimaryStreamingModelCannotBeCreated() {
        ModelTarget secondary = new ModelTarget("backup", "backup-model", 5000, 0, 0);
        when(router.resolveChain(AiTaskType.RAG)).thenReturn(List.of(target, secondary));
        when(factory.get(target)).thenThrow(new IllegalStateException("primary unavailable"));
        ChatModel chatModel = mock(ChatModel.class);
        StreamingChatModel streamingModel = mock(StreamingChatModel.class);
        when(factory.get(secondary)).thenReturn(new GatewayChatModels(chatModel, streamingModel));
        doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onPartialResponse("backup answer");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("backup answer")).build());
            return null;
        }).when(streamingModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

        List<ModelStreamChunk> chunks = gateway().stream(AiTaskType.RAG, "system", "question", null, "guest")
                .collectList().block();

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getDelta()).isEqualTo("backup answer");
        assertThat(chunks.get(0).isFallbackUsed()).isTrue();
        verify(health).recordFailure("test", "primary unavailable");
    }

    @Test
    void stopsForwardingCallbacksAfterSubscriberCancels() {
        ChatModel chatModel = mock(ChatModel.class);
        StreamingChatModel streamingModel = mock(StreamingChatModel.class);
        when(factory.get(target)).thenReturn(new GatewayChatModels(chatModel, streamingModel));
        doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onPartialResponse("first");
            handler.onPartialResponse("ignored-after-cancel");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("first ignored-after-cancel")).build());
            return null;
        }).when(streamingModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

        List<ModelStreamChunk> chunks = gateway().stream(AiTaskType.RAG, "system", "question", null, "guest")
                .take(1).collectList().block();

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getDelta()).isEqualTo("first");
    }

    private AIGatewayService gateway() {
        return new AIGatewayService(properties, router, factory, health, quota, guard, logProducer, logMapper);
    }

    private static void assertChunk(ModelStreamChunk chunk, String delta, int inputTokens, int outputTokens) {
        assertThat(chunk.getDelta()).isEqualTo(delta);
        assertThat(chunk.getInputTokens()).isEqualTo(inputTokens);
        assertThat(chunk.getOutputTokens()).isEqualTo(outputTokens);
        assertThat(chunk.getProvider()).isEqualTo("test");
        assertThat(chunk.getModel()).isEqualTo("test-model");
    }
}
