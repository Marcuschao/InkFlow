package com.blog.ai.gateway.service;

import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.gateway.AiTaskType;
import com.blog.ai.gateway.config.GatewayProperties;
import com.blog.ai.gateway.context.GatewayContext;
import com.blog.ai.gateway.context.GatewayUserContext;
import com.blog.ai.gateway.factory.ChatModelFactory;
import com.blog.ai.gateway.factory.GatewayChatModels;
import com.blog.ai.gateway.guard.PromptGuard;
import com.blog.ai.gateway.health.ModelHealthChecker;
import com.blog.ai.gateway.model.GatewayResult;
import com.blog.ai.gateway.model.ModelStreamChunk;
import com.blog.ai.gateway.model.ModelTarget;
import com.blog.ai.gateway.mq.AiCallLogEvent;
import com.blog.ai.gateway.mq.AiCallLogProducer;
import com.blog.ai.gateway.quota.TokenQuotaService;
import com.blog.ai.gateway.router.ModelRouter;
import com.blog.ai.mapper.AiCallLogMapper;
import com.blog.ai.model.entity.AiCallLog;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AIGatewayService {

    private static final String FALLBACK = "AI 服务暂时不可用，请稍后再试。";

    private final GatewayProperties gatewayProperties;
    private final ModelRouter modelRouter;
    private final ChatModelFactory chatModelFactory;
    private final ModelHealthChecker healthChecker;
    private final TokenQuotaService tokenQuotaService;
    private final PromptGuard promptGuard;
    private final AiCallLogProducer aiCallLogProducer;
    private final AiCallLogMapper aiCallLogMapper;

    public AIGatewayService(GatewayProperties gatewayProperties, ModelRouter modelRouter,
                            ChatModelFactory chatModelFactory, ModelHealthChecker healthChecker,
                            TokenQuotaService tokenQuotaService, PromptGuard promptGuard,
                            AiCallLogProducer aiCallLogProducer, AiCallLogMapper aiCallLogMapper) {
        this.gatewayProperties = gatewayProperties;
        this.modelRouter = modelRouter;
        this.chatModelFactory = chatModelFactory;
        this.healthChecker = healthChecker;
        this.tokenQuotaService = tokenQuotaService;
        this.promptGuard = promptGuard;
        this.aiCallLogProducer = aiCallLogProducer;
        this.aiCallLogMapper = aiCallLogMapper;
    }

    public GatewayResult chat(AiTaskType taskType, String systemPrompt, String userPrompt) {
        Long userId = GatewayUserContext.getUserId();
        tokenQuotaService.checkQuota(userId, taskType);
        promptGuard.checkInput(userPrompt);
        List<ModelTarget> chain = modelRouter.resolveChain(taskType);
        if (chain.isEmpty()) return failedResult(taskType, userId, "No model is available", 0);

        boolean fallbackUsed = false;
        Exception lastEx = null;
        for (int i = 0; i < chain.size(); i++) {
            ModelTarget target = chain.get(i);
            fallbackUsed = i > 0;
            long started = System.currentTimeMillis();
            try {
                GatewayResult result = invoke(target, systemPrompt, userPrompt);
                result.setFallbackUsed(fallbackUsed);
                result.setLatencyMs(System.currentTimeMillis() - started);
                result.setContent(promptGuard.filterOutput(result.getContent()));
                tokenQuotaService.recordUsage(userId, result.getTotalTokens());
                GatewayContext.setModelUsed(target.getProviderId() + "/" + target.getModel());
                healthChecker.recordSuccess(target.getProviderId());
                publishLog(taskType, userId, result);
                return result;
            } catch (Exception ex) {
                lastEx = ex;
                healthChecker.recordFailure(target.getProviderId(), ex.getMessage());
            }
        }
        GatewayResult failed = failedResult(taskType, userId,
                lastEx == null ? "All providers failed" : lastEx.getMessage(), 0);
        failed.setContent(FALLBACK);
        failed.setStatus(fallbackUsed ? "fallback" : "failed");
        failed.setFallbackUsed(fallbackUsed);
        publishLog(taskType, userId, failed);
        return failed;
    }

    public Flux<ModelStreamChunk> stream(AiTaskType taskType, String systemPrompt, String userPrompt) {
        return stream(taskType, systemPrompt, userPrompt,
                GatewayUserContext.getUserId(), GatewayUserContext.getUsername());
    }

    public Flux<ModelStreamChunk> stream(AiTaskType taskType, String systemPrompt, String userPrompt,
                                         Long userId, String username) {
        return Flux.defer(() -> {
            tokenQuotaService.checkQuota(userId, taskType);
            promptGuard.checkInput(userPrompt);
            List<ModelTarget> chain = modelRouter.resolveChain(taskType);
            if (chain.isEmpty()) return Flux.error(new ServiceException(503, "No streaming model is available"));
            return streamFrom(chain, 0, taskType, userId, username, systemPrompt, userPrompt);
        });
    }

    private Flux<ModelStreamChunk> streamFrom(List<ModelTarget> chain, int index, AiTaskType taskType,
                                               Long userId, String username, String systemPrompt, String userPrompt) {
        if (index >= chain.size()) return Flux.error(new ServiceException(503, "All streaming models failed"));
        ModelTarget target = chain.get(index);
        GatewayChatModels models;
        try {
            models = chatModelFactory.get(target);
        } catch (Exception error) {
            healthChecker.recordFailure(target.getProviderId(), error.getMessage());
            return streamFrom(chain, index + 1, taskType, userId, username, systemPrompt, userPrompt);
        }
        StreamingChatModel streamingModel = models.streamingChatModel();
        AtomicInteger inputTokens = new AtomicInteger();
        AtomicInteger outputTokens = new AtomicInteger();
        AtomicBoolean emitted = new AtomicBoolean();
        AtomicBoolean cancelled = new AtomicBoolean();
        long started = System.currentTimeMillis();

        Flux<ModelStreamChunk> source = Flux.create(sink -> {
            sink.onCancel(() -> cancelled.set(true));
            sink.onDispose(() -> cancelled.set(true));
            try {
                streamingModel.chat(request(systemPrompt, userPrompt), new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String text) {
                        if (cancelled.get() || !StringUtils.hasText(text)) return;
                        String filtered = promptGuard.filterOutput(text);
                        if (!StringUtils.hasText(filtered)) return;
                        emitted.set(true);
                        sink.next(ModelStreamChunk.builder().delta(filtered)
                                .provider(target.getProviderId()).model(target.getModel())
                                .fallbackUsed(index > 0).build());
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        if (cancelled.get()) return;
                        TokenUsage usage = response == null ? null : response.tokenUsage();
                        if (usage != null) {
                            inputTokens.set(safeInt(usage.inputTokenCount()));
                            outputTokens.set(safeInt(usage.outputTokenCount()));
                        }
                        sink.next(ModelStreamChunk.builder().delta("")
                                .provider(target.getProviderId()).model(target.getModel())
                                .inputTokens(inputTokens.get()).outputTokens(outputTokens.get())
                                .cost(calcCost(target, inputTokens.get(), outputTokens.get()))
                                .fallbackUsed(index > 0).build());
                        sink.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (!cancelled.get()) sink.error(error);
                    }
                });
            } catch (Throwable error) {
                if (!cancelled.get()) sink.error(error);
            }
        });

        return source.timeout(Duration.ofMillis(target.getTimeoutMs() > 0
                        ? target.getTimeoutMs() : gatewayProperties.getDefaultTimeoutMs()))
                .doOnNext(chunk -> {
                    if (chunk.getInputTokens() > 0) inputTokens.set(chunk.getInputTokens());
                    if (chunk.getOutputTokens() > 0) outputTokens.set(chunk.getOutputTokens());
                })
                .doOnComplete(() -> {
                    if (!emitted.get()) return;
                    healthChecker.recordSuccess(target.getProviderId());
                    tokenQuotaService.recordUsage(userId, inputTokens.get() + outputTokens.get());
                    GatewayResult result = new GatewayResult();
                    result.setProvider(target.getProviderId());
                    result.setModel(target.getModel());
                    result.setStatus("success");
                    result.setInputTokens(inputTokens.get());
                    result.setOutputTokens(outputTokens.get());
                    result.setCost(calcCost(target, inputTokens.get(), outputTokens.get()));
                    result.setLatencyMs(System.currentTimeMillis() - started);
                    result.setFallbackUsed(index > 0);
                    publishLog(taskType, userId, username, result);
                })
                .onErrorResume(error -> {
                    healthChecker.recordFailure(target.getProviderId(), error.getMessage());
                    if (emitted.get()) return Flux.error(error);
                    return streamFrom(chain, index + 1, taskType, userId, username, systemPrompt, userPrompt);
                });
    }

    private GatewayResult invoke(ModelTarget target, String systemPrompt, String userPrompt) throws Exception {
        GatewayChatModels models = chatModelFactory.get(target);
        long timeout = target.getTimeoutMs() > 0 ? target.getTimeoutMs() : gatewayProperties.getDefaultTimeoutMs();
        CompletableFuture<ChatResponse> future = CompletableFuture.supplyAsync(() ->
                models.chatModel().chat(request(systemPrompt, userPrompt)));
        ChatResponse response;
        try {
            response = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new ServiceException(504, "LLM request timed out");
        }
        if (response == null || response.aiMessage() == null) {
            throw new ServiceException(502, "LLM returned an empty response");
        }
        String content = response.aiMessage().text();
        if (!StringUtils.hasText(content)) {
            throw new ServiceException(502, "LLM returned an empty response");
        }
        GatewayResult result = new GatewayResult();
        result.setContent(content.trim());
        result.setProvider(target.getProviderId());
        result.setModel(target.getModel());
        result.setStatus("success");
        TokenUsage usage = response.tokenUsage();
        if (usage != null) {
            result.setInputTokens(safeInt(usage.inputTokenCount()));
            result.setOutputTokens(safeInt(usage.outputTokenCount()));
        }
        result.setCost(calcCost(target, result.getInputTokens(), result.getOutputTokens()));
        return result;
    }

    private ChatRequest request(String systemPrompt, String userPrompt) {
        List<ChatMessage> messages = List.of(SystemMessage.from(nullToEmpty(systemPrompt)),
                UserMessage.from(nullToEmpty(userPrompt)));
        return ChatRequest.builder().messages(messages).build();
    }

    private GatewayResult failedResult(AiTaskType taskType, Long userId, String error, long latency) {
        GatewayResult result = new GatewayResult();
        result.setStatus("failed");
        result.setErrorMsg(error);
        result.setLatencyMs(latency);
        result.setContent(FALLBACK);
        return result;
    }

    private void publishLog(AiTaskType taskType, Long userId, GatewayResult result) {
        publishLog(taskType, userId, GatewayUserContext.getUsername(), result);
    }

    private void publishLog(AiTaskType taskType, Long userId, String username, GatewayResult result) {
        AiCallLogEvent event = new AiCallLogEvent();
        event.setUserId(userId);
        event.setUsername(username);
        event.setTaskType(taskType.code());
        event.setFeature(taskType.code());
        event.setProvider(result.getProvider());
        event.setModel(result.getModel());
        event.setInputTokens(result.getInputTokens());
        event.setOutputTokens(result.getOutputTokens());
        event.setCost(result.getCost());
        event.setLatencyMs(result.getLatencyMs());
        event.setStatus(result.getStatus());
        event.setErrorMsg(result.getErrorMsg());
        event.setSuccess("success".equals(result.getStatus()) ? 1 : 0);
        event.setCreatedAt(LocalDateTime.now());
        if (gatewayProperties.isLogAsync()) {
            try {
                aiCallLogProducer.send(event);
                return;
            } catch (Exception ignored) {
            }
        }
        writeSync(event);
    }

    private void writeSync(AiCallLogEvent event) {
        try {
            AiCallLog row = new AiCallLog();
            row.setUserId(event.getUserId());
            row.setUsername(event.getUsername());
            row.setTaskType(event.getTaskType());
            row.setFeature(event.getFeature() != null ? event.getFeature() : event.getTaskType());
            row.setProvider(event.getProvider());
            row.setModel(event.getModel());
            row.setInputTokens(event.getInputTokens());
            row.setOutputTokens(event.getOutputTokens());
            row.setCost(event.getCost());
            row.setLatencyMs(event.getLatencyMs());
            row.setStatus(event.getStatus());
            row.setErrorMsg(event.getErrorMsg());
            row.setSuccess(event.getSuccess());
            row.setDurationMs(event.getLatencyMs());
            row.setCreatedAt(event.getCreatedAt());
            aiCallLogMapper.insert(row);
        } catch (Exception ignored) {
        }
    }

    public String chatContent(AiTaskType taskType, String systemPrompt, String userPrompt) {
        GatewayResult result = chat(taskType, systemPrompt, userPrompt);
        return StringUtils.hasText(result.getContent()) ? result.getContent() : FALLBACK;
    }

    private double calcCost(ModelTarget target, int input, int output) {
        return (input * target.getInputPricePer1k() + output * target.getOutputPricePer1k()) / 1000.0;
    }

    private int safeInt(Number value) {
        return value == null ? 0 : value.intValue();
    }

    public static String hashPrompt(String system, String user) {
        String raw = nullToEmpty(system) + "\n" + nullToEmpty(user);
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
