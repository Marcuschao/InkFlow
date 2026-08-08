package com.blog.ai.runtime.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AgentRequest {
    @NotNull
    private AgentTaskType taskType;
    private String operation;
    private String question;
    private Long articleId;
    private Long sessionId;
    private Map<String, Object> input = new LinkedHashMap<>();
    private Integer maxSteps;
    private Integer tokenBudget;
    private Long timeoutMs;
}
