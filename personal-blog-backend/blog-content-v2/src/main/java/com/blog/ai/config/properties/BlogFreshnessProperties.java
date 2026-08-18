package com.blog.ai.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "blog.freshness")
public class BlogFreshnessProperties {
    private int warnDays = 180;
    private int severeDays = 365;
}
