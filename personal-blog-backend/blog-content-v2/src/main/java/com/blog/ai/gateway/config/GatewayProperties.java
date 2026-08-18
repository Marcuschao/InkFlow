package com.blog.ai.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.ArrayList;
import java.util.List;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "blog.ai.gateway")
public class GatewayProperties {

    private boolean enabled = true;
    /**
     * When enabled, ai_model_config and ai_route_rule override external configuration.
     * Disable this when Nacos is the single source of truth for model routing.
     */
    private boolean databaseOverridesEnabled = true;
    private long defaultTimeoutMs = 5000;
    private boolean healthCheckEnabled = false;
    private String apiKeyCipherSecret;
    private long healthCheckIntervalMs = 30000;
    private long halfOpenIntervalMs = 60000;
    private int failureThreshold = 3;
    private boolean logAsync = true;
    private List<ProviderDef> providers = new ArrayList<>();
    private List<RouteDef> routes = new ArrayList<>();
    private QuotaDef quota = new QuotaDef();

    @Data
    public static class ProviderDef {
        private String id;
        private String name;
        private boolean enabled = true;
        private String apiKey;
        private String baseUrl;
        private List<String> models = new ArrayList<>();
        private int priority = 1;
        private int maxConcurrency = 10;
        private long timeoutMs = 5000;
        private double temperature = 0.7;
        private int maxTokens = 1024;
        private double inputPricePer1k = 0.001;
        private double outputPricePer1k = 0.002;
    }

    @Data
    public static class RouteDef {
        private String taskType;
        private String primary;
        private List<String> fallbacks = new ArrayList<>();
    }

    @Data
    public static class QuotaDef {
        private long globalDailyTokens = 1_000_000;
        private long userDailyTokens = 50_000;
        private List<Long> whitelistUserIds = new ArrayList<>();
        private List<String> coreTaskTypes = List.of("agent", "rag", "chat");
    }
}
