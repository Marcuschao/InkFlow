package com.blog.personalblogbackend.config.interaction;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.interaction")
public class InteractionProperties {
    private int idempotencyTtlSeconds = 60;
    private int flushIntervalSeconds = 30;
    private boolean redisCountEnabled = true;
    private boolean syncFallback = true;
    private String exchange = "blog.interaction";
    private String queue = "interaction.persist.queue";
    private String routingKey = "interaction.persist";
}
