package com.blog.personalblogbackend.config.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.cache")
public class CacheProperties {
    private int l1MaxSize = 1000;
    private int l1WriteTtlMinutes = 5;
    private int nullTtlSeconds = 30;
    private int listCacheMaxPage = 3;
    private long evictDelayMs = 500;
}
