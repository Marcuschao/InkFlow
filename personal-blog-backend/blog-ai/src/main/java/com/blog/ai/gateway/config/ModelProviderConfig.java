package com.blog.ai.gateway.config;

import com.blog.ai.gateway.model.ModelTarget;
import com.blog.ai.mapper.AiModelConfigMapper;
import com.blog.ai.model.entity.AiModelConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ModelProviderConfig {

    private final GatewayProperties gatewayProperties;
    private final AiModelConfigMapper aiModelConfigMapper;
    private final String fallbackApiKey;
    private final String fallbackBaseUrl;
    private final String fallbackModel;

    private volatile Map<String, GatewayProperties.ProviderDef> providerMap = new ConcurrentHashMap<>();

    public ModelProviderConfig(GatewayProperties gatewayProperties,
                               AiModelConfigMapper aiModelConfigMapper,
                               @Value("${spring.ai.openai.api-key:}") String fallbackApiKey,
                               @Value("${spring.ai.openai.base-url:https://api.openai.com}") String fallbackBaseUrl,
                               @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String fallbackModel) {
        this.gatewayProperties = gatewayProperties;
        this.aiModelConfigMapper = aiModelConfigMapper;
        this.fallbackApiKey = fallbackApiKey;
        this.fallbackBaseUrl = fallbackBaseUrl;
        this.fallbackModel = fallbackModel;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onConfigRefresh(EnvironmentChangeEvent event) {
        if (event.getKeys().stream().anyMatch(k -> k.startsWith("blog.ai.gateway") || k.startsWith("spring.ai.openai"))) {
            reload();
        }
    }

    public void reload() {
        Map<String, GatewayProperties.ProviderDef> map = new LinkedHashMap<>();
        List<GatewayProperties.ProviderDef> defs = new ArrayList<>(gatewayProperties.getProviders());
        if (defs.isEmpty()) {
            GatewayProperties.ProviderDef def = new GatewayProperties.ProviderDef();
            def.setId("deepseek");
            def.setName("deepseek");
            def.setEnabled(true);
            def.setApiKey(fallbackApiKey);
            def.setBaseUrl(fallbackBaseUrl);
            def.setModels(List.of(fallbackModel));
            def.setPriority(1);
            defs.add(def);
        }
        for (GatewayProperties.ProviderDef def : defs) {
            if (!StringUtils.hasText(def.getApiKey())) {
                def.setApiKey(fallbackApiKey);
            }
            if (!StringUtils.hasText(def.getBaseUrl())) {
                def.setBaseUrl(fallbackBaseUrl);
            }
            if (def.getModels() == null || def.getModels().isEmpty()) {
                def.setModels(List.of(fallbackModel));
            }
            map.put(def.getId(), def);
        }
        applyDbOverrides(map);
        providerMap = map;
    }

    private void applyDbOverrides(Map<String, GatewayProperties.ProviderDef> map) {
        try {
            List<AiModelConfig> rows = aiModelConfigMapper.selectList(null);
            if (rows == null) {
                return;
            }
            for (AiModelConfig row : rows) {
                GatewayProperties.ProviderDef def = map.get(row.getProviderId());
                if (def == null) {
                    continue;
                }
                if (row.getEnabled() != null) {
                    def.setEnabled(row.getEnabled() == 1);
                }
                if (row.getPriority() != null) {
                    def.setPriority(row.getPriority());
                }
                if (row.getMaxConcurrency() != null) {
                    def.setMaxConcurrency(row.getMaxConcurrency());
                }
                if (row.getTimeoutMs() != null) {
                    def.setTimeoutMs(row.getTimeoutMs());
                }
            }
        } catch (Exception ignored) {
        }
    }

    public GatewayProperties.ProviderDef getProvider(String providerId) {
        return providerMap.get(providerId);
    }

    public List<GatewayProperties.ProviderDef> listEnabledProviders() {
        return providerMap.values().stream()
                .filter(GatewayProperties.ProviderDef::isEnabled)
                .sorted(Comparator.comparingInt(GatewayProperties.ProviderDef::getPriority))
                .collect(Collectors.toList());
    }

    public ModelTarget parseTarget(String spec) {
        if (!StringUtils.hasText(spec)) {
            return null;
        }
        String[] parts = spec.split(":", 2);
        if (parts.length != 2) {
            return null;
        }
        GatewayProperties.ProviderDef def = providerMap.get(parts[0]);
        if (def == null || !def.isEnabled()) {
            return null;
        }
        return new ModelTarget(parts[0], parts[1], def.getTimeoutMs(),
                def.getInputPricePer1k(), def.getOutputPricePer1k());
    }

    public ModelTarget defaultTarget() {
        GatewayProperties.ProviderDef def = listEnabledProviders().stream().findFirst().orElse(null);
        if (def == null) {
            return null;
        }
        String model = def.getModels().get(0);
        return new ModelTarget(def.getId(), model, def.getTimeoutMs(),
                def.getInputPricePer1k(), def.getOutputPricePer1k());
    }
}
