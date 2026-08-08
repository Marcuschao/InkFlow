package com.blog.ai.runtime.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_agent_tool_call")
public class AgentToolCall {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String toolCallId;
    private String runId;
    private String traceId;
    private String toolName;
    private String toolVersion;
    private String riskLevel;
    private String idempotencyKey;
    private String argumentsDigest;
    private String validationStatus;
    private String approvalStatus;
    private String executionStatus;
    private String resultDigest;
    private Long durationMs;
    private Long operatorUserId;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
