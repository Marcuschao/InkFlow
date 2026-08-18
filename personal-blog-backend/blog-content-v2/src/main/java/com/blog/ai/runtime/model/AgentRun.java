package com.blog.ai.runtime.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_agent_run")
public class AgentRun {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String runId;
    private String traceId;
    private Long sessionId;
    private Long userId;
    private String guestIdHash;
    private String tenantId;
    private String taskType;
    private String agentName;
    private String agentVersion;
    private String promptVersion;
    private String model;
    private String status;
    private Integer stepCount;
    private Integer inputTokens;
    private Integer outputTokens;
    private Double cost;
    private String errorCode;
    private String errorMessage;
    private String resultJson;
    private Integer version;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
