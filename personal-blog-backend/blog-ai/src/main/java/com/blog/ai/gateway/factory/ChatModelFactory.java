package com.blog.ai.gateway.factory;

import com.blog.ai.gateway.config.GatewayProperties;
import com.blog.ai.gateway.config.ModelProviderConfig;
import com.blog.ai.gateway.model.ModelTarget;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatModelFactory {

    private final ModelProviderConfig modelProviderConfig;
    private final Map<String, ChatModel> cache = new ConcurrentHashMap<>();

    public ChatModelFactory(ModelProviderConfig modelProviderConfig) {
        this.modelProviderConfig = modelProviderConfig;
    }

    public ChatModel get(ModelTarget target) {
        return cache.computeIfAbsent(target.key(), k -> build(target));
    }

    public void invalidateAll() {
        cache.clear();
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onConfigRefresh(EnvironmentChangeEvent event) {
        if (event.getKeys().stream().anyMatch(key -> key.startsWith("blog.ai.gateway")
                || key.startsWith("spring.ai.openai"))) {
            invalidateAll();
        }
    }

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefreshScopeRefreshed(RefreshScopeRefreshedEvent event) {
        invalidateAll();
    }

    private ChatModel build(ModelTarget target) {
        GatewayProperties.ProviderDef def = modelProviderConfig.getProvider(target.getProviderId());
        if (def == null || !StringUtils.hasText(def.getApiKey())) {
            throw new IllegalStateException("Provider 未配置 API Key: " + target.getProviderId());
        }
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder().apiKey(def.getApiKey());
        if (StringUtils.hasText(def.getBaseUrl())) {
            String normalized = def.getBaseUrl().endsWith("/")
                    ? def.getBaseUrl().substring(0, def.getBaseUrl().length() - 1)
                    : def.getBaseUrl();
            apiBuilder.baseUrl(normalized);
        }
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(target.getModel())
                .temperature(0.7)
                .maxTokens(1024)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(apiBuilder.build())
                .defaultOptions(options)
                .build();
    }
}
