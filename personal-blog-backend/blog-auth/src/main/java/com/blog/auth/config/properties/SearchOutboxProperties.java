package com.blog.auth.config.properties;

import lombok.Data;

@Data
public class SearchOutboxProperties {
    private long pollIntervalMs = 5000;
    private int batchSize = 100;
    private int maxRetries = 10;
}
