package com.blog.ai.runtime.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "blog.agent.runtime")
public class AgentRuntimeProperties {
    private int maxSteps = 12;
    private int tokenBudget = 16_000;
    private Duration totalTimeout = Duration.ofMinutes(2);
    private Duration stepTimeout = Duration.ofSeconds(45);
    private Duration eventRetention = Duration.ofHours(24);
    private int eventReplayLimit = 2_000;
    private int runtimeCorePoolSize = 8;
    private int runtimeMaxPoolSize = 32;
    private int toolCorePoolSize = 4;
    private int toolMaxPoolSize = 16;
    private int eventCorePoolSize = 2;
    private int eventMaxPoolSize = 4;
    private int queueCapacity = 500;
}
