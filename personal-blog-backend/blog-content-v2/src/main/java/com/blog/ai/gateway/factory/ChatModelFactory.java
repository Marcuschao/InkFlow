package com.blog.ai.gateway.factory;

import com.blog.ai.gateway.config.GatewayProperties;
import com.blog.ai.gateway.config.ModelProviderConfig;
import com.blog.ai.gateway.model.ModelTarget;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatModelFactory {

    private final ModelProviderConfig modelProviderConfig;
    private final Map<String, GatewayChatModels> cache = new ConcurrentHashMap<>();

    public ChatModelFactory(ModelProviderConfig modelProviderConfig) {
        this.modelProviderConfig = modelProviderConfig;
    }

    public GatewayChatModels get(ModelTarget target) {
        return cache.computeIfAbsent(target.key(), key -> build(target));
    }

    public void invalidateAll() {
        cache.clear();
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onConfigRefresh(EnvironmentChangeEvent event) {
        if (event.getKeys().stream().anyMatch(key -> key.startsWith("blog.ai.gateway"))) {
            invalidateAll();
        }
    }

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefreshScopeRefreshed(RefreshScopeRefreshedEvent event) {
        invalidateAll();
    }

    private GatewayChatModels build(ModelTarget target) {
        GatewayProperties.ProviderDef def = modelProviderConfig.getProvider(target.getProviderId());
        if (def == null || !StringUtils.hasText(def.getApiKey())) {
            throw new IllegalStateException("Provider missing API key: " + target.getProviderId());
        }
        if (!StringUtils.hasText(def.getBaseUrl())) {
            throw new IllegalStateException("Provider missing base URL: " + target.getProviderId());
        }
        if (!StringUtils.hasText(target.getModel())) {
            throw new IllegalStateException("Provider missing model: " + target.getProviderId());
        }
        String baseUrl = normalizeBaseUrl(def.getBaseUrl());
        long timeoutMs = target.getTimeoutMs() > 0 ? target.getTimeoutMs() : def.getTimeoutMs();
        Duration timeout = Duration.ofMillis(Math.max(timeoutMs, 1));
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(def.getApiKey())
                .baseUrl(baseUrl)
                .modelName(target.getModel())
                .temperature(def.getTemperature())
                .maxTokens(def.getMaxTokens())
                .timeout(timeout)
                .maxRetries(0)
                .build();
        OpenAiStreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .apiKey(def.getApiKey())
                .baseUrl(baseUrl)
                .modelName(target.getModel())
                .temperature(def.getTemperature())
                .maxTokens(def.getMaxTokens())
                .timeout(timeout)
                .build();
        return new GatewayChatModels(chatModel, streamingChatModel);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
