package com.blog.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_call_log")
public class AiCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String feature;
    private Integer success;
    private Long durationMs;
    private String username;
    private String clientIp;
    private Long userId;
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
    private LocalDateTime createdAt;
}
