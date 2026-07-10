package com.blog.ai.gateway.model;

import lombok.Data;

@Data
public class GatewayResult {
    private String content;
    private String provider;
    private String model;
    private int inputTokens;
    private int outputTokens;
    private double cost;
    private long latencyMs;
    private String status = "success";
    private String errorMsg;
    private boolean fallbackUsed;

    public int getTotalTokens() {
        return inputTokens + outputTokens;
    }
}
