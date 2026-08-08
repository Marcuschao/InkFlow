package com.blog.ai.gateway.service;

import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.gateway.AiTaskType;
import com.blog.ai.gateway.config.GatewayProperties;
import com.blog.ai.gateway.context.GatewayContext;
import com.blog.ai.gateway.context.GatewayUserContext;
import com.blog.ai.gateway.factory.ChatModelFactory;
import com.blog.ai.gateway.guard.PromptGuard;
import com.blog.ai.gateway.health.ModelHealthChecker;
import com.blog.ai.gateway.model.GatewayResult;
import com.blog.ai.gateway.model.ModelTarget;
import com.blog.ai.gateway.model.ModelStreamChunk;
import com.blog.ai.gateway.mq.AiCallLogEvent;
import com.blog.ai.gateway.mq.AiCallLogProducer;
import com.blog.ai.gateway.quota.TokenQuotaService;
import com.blog.ai.gateway.router.ModelRouter;
import com.blog.ai.mapper.AiCallLogMapper;
import com.blog.ai.model.entity.AiCallLog;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import reactor.core.publisher.Flux;

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

    public AIGatewayService(GatewayProperties gatewayProperties,
                            ModelRouter modelRouter,
                            ChatModelFactory chatModelFactory,
                            ModelHealthChecker healthChecker,
                            TokenQuotaService tokenQuotaService,
                            PromptGuard promptGuard,
                            AiCallLogProducer aiCallLogProducer,
                            AiCallLogMapper aiCallLogMapper) {
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
        if (chain.isEmpty()) {
            return failedResult(taskType, userId, "无可用模型", 0);
        }

        boolean fallbackUsed = false;
        Exception lastEx = null;
        for (int i = 0; i < chain.size(); i++) {
            ModelTarget target = chain.get(i);
            if (i > 0) {
                fallbackUsed = true;
            }
            long t0 = System.currentTimeMillis();
            try {
                GatewayResult result = invoke(target, systemPrompt, userPrompt);
                result.setFallbackUsed(fallbackUsed);
                result.setLatencyMs(System.currentTimeMillis() - t0);
                result.setContent(promptGuard.filterOutput(result.getContent()));
                tokenQuotaService.recordUsage(userId, result.getTotalTokens());
                GatewayContext.setModelUsed(target.getProviderId() + "/" + target.getModel());
                healthChecker.recordSuccess(target.getProviderId());
                publishLog(taskType, userId, result);
                return result;
            } catch (Exception e) {
                lastEx = e;
                healthChecker.recordFailure(target.getProviderId(), e.getMessage());
            }
        }

        GatewayResult fail = failedResult(taskType, userId,
                lastEx != null ? lastEx.getMessage() : "all providers failed",
                chain.get(chain.size() - 1) != null ? 0 : 0);
        fail.setContent(FALLBACK);
        fail.setStatus(fallbackUsed ? "fallback" : "failed");
        fail.setFallbackUsed(fallbackUsed);
        publishLog(taskType, userId, fail);
        return fail;
    }

    public Flux<ModelStreamChunk> stream(AiTaskType taskType, String systemPrompt, String userPrompt) {
        return stream(taskType, systemPrompt, userPrompt, GatewayUserContext.getUserId(), GatewayUserContext.getUsername());
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
        ChatModel chatModel = chatModelFactory.get(target);
        Prompt prompt = new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)));
        AtomicInteger inputTokens = new AtomicInteger();
        AtomicInteger outputTokens = new AtomicInteger();
        AtomicBoolean emitted = new AtomicBoolean();
        long started = System.currentTimeMillis();
        return chatModel.stream(prompt)
                .timeout(java.time.Duration.ofMillis(target.getTimeoutMs() > 0
                        ? target.getTimeoutMs() : gatewayProperties.getDefaultTimeoutMs()))
                .map(response -> {
                    Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;
                    if (usage != null) {
                        inputTokens.set(safeInt(usage.getPromptTokens()));
                        outputTokens.set(safeInt(usage.getCompletionTokens()));
                    }
                    String text = response.getResult() != null && response.getResult().getOutput() != null
                            ? response.getResult().getOutput().getText() : null;
                    text = promptGuard.filterOutput(text);
                    emitted.set(emitted.get() || StringUtils.hasText(text));
                    return ModelStreamChunk.builder().delta(text).provider(target.getProviderId()).model(target.getModel())
                            .inputTokens(inputTokens.get()).outputTokens(outputTokens.get())
                            .cost(calcCost(target, inputTokens.get(), outputTokens.get())).fallbackUsed(index > 0).build();
                })
                .filter(chunk -> StringUtils.hasText(chunk.getDelta()))
                .doOnComplete(() -> {
                    if (!emitted.get()) return;
                    healthChecker.recordSuccess(target.getProviderId());
                    tokenQuotaService.recordUsage(userId, inputTokens.get() + outputTokens.get());
                    GatewayResult result = new GatewayResult();
                    result.setProvider(target.getProviderId()); result.setModel(target.getModel()); result.setStatus("success");
                    result.setInputTokens(inputTokens.get()); result.setOutputTokens(outputTokens.get());
                    result.setCost(calcCost(target, inputTokens.get(), outputTokens.get()));
                    result.setLatencyMs(System.currentTimeMillis() - started); result.setFallbackUsed(index > 0);
                    publishLog(taskType, userId, username, result);
                })
                .onErrorResume(error -> {
                    healthChecker.recordFailure(target.getProviderId(), error.getMessage());
                    if (emitted.get()) return Flux.error(error);
                    return streamFrom(chain, index + 1, taskType, userId, username, systemPrompt, userPrompt);
                });
    }

    private GatewayResult invoke(ModelTarget target, String systemPrompt, String userPrompt) throws Exception {
        ChatModel chatModel = chatModelFactory.get(target);
        long timeout = target.getTimeoutMs() > 0 ? target.getTimeoutMs() : gatewayProperties.getDefaultTimeoutMs();
        CompletableFuture<ChatResponse> future = CompletableFuture.supplyAsync(() ->
                chatModel.call(new Prompt(List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userPrompt)))));
        ChatResponse response;
        try {
            response = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ServiceException(504, "LLM 请求超时");
        }
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new ServiceException(502, "LLM 返回空响应");
        }
        String content = response.getResult().getOutput().getText();
        if (!StringUtils.hasText(content)) {
            throw new ServiceException(502, "LLM 返回空响应");
        }
        GatewayResult result = new GatewayResult();
        result.setContent(content.trim());
        result.setProvider(target.getProviderId());
        result.setModel(target.getModel());
        result.setStatus("success");
        Usage usage = response.getMetadata() != null ? response.getMetadata().getUsage() : null;
        if (usage != null) {
            result.setInputTokens(safeInt(usage.getPromptTokens()));
            result.setOutputTokens(safeInt(usage.getCompletionTokens()));
        }
        result.setCost(calcCost(target, result.getInputTokens(), result.getOutputTokens()));
        return result;
    }

    private GatewayResult failedResult(AiTaskType taskType, Long userId, String error, long latency) {
        GatewayResult r = new GatewayResult();
        r.setStatus("failed");
        r.setErrorMsg(error);
        r.setLatencyMs(latency);
        r.setContent(FALLBACK);
        return r;
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
        if (!StringUtils.hasText(result.getContent())) {
            return FALLBACK;
        }
        return result.getContent();
    }

    private double calcCost(ModelTarget target, int input, int output) {
        return (input * target.getInputPricePer1k() + output * target.getOutputPricePer1k()) / 1000.0;
    }

    private int safeInt(Number v) {
        return v == null ? 0 : v.intValue();
    }

    public static String hashPrompt(String system, String user) {
        String raw = nullToEmpty(system) + "\n" + nullToEmpty(user);
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
