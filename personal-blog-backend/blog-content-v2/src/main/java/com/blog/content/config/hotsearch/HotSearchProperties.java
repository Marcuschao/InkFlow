package com.blog.content.config.hotsearch;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.hot-search")
public class HotSearchProperties {
    private int cacheTtlMinutes = 5;
    private int connectTimeoutSeconds = 5;
    private int readTimeoutSeconds = 10;
    private int homePreviewSize = 5;
}
