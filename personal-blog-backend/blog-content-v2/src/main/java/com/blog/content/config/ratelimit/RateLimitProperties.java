package com.blog.content.config.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private int articleDetailPerMinute = 120;
    private int articleListPerMinute = 180;
    private int apiGetPerMinute = 300;
    private int apiWritePerMinute = 30;
    private int adminPerMinute = 120;
    private int statViewPerMinute = 300;
}
