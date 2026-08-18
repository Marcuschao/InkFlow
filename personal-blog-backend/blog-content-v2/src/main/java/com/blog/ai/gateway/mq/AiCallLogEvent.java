package com.blog.ai.gateway.mq;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiCallLogEvent {
    private Long userId;
    private String username;
    private String taskType;
    private String provider;
    private String model;
    private Integer inputTokens;
    private Integer outputTokens;
    private Double cost;
    private Long latencyMs;
    private String status;
    private String errorMsg;
    private String promptHash;
    private String feature;
    private Integer success;
    private LocalDateTime createdAt;
}
