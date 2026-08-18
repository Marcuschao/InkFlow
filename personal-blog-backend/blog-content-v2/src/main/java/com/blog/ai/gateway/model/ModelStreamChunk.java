package com.blog.ai.gateway.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelStreamChunk {
    private String delta;
    private String provider;
    private String model;
    private int inputTokens;
    private int outputTokens;
    private double cost;
    private boolean fallbackUsed;
}
