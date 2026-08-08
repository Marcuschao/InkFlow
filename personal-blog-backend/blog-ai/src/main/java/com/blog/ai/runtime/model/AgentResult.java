package com.blog.ai.runtime.model;

import com.blog.ai.model.dto.agent.ChatSourceDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class AgentResult {
    private String runId;
    private String traceId;
    private AgentRunStatus status;
    private String answer;
    private Map<String, Object> output;
    private List<ChatSourceDto> sources = new ArrayList<>();
    private Long sessionId;
    private Long messageId;
    private boolean grounded;
    private double confidence;
    private String refusalReason;
    private boolean degraded;
    private Integer inputTokens = 0;
    private Integer outputTokens = 0;
    private Double cost = 0D;
    private String model;
    private String errorCode;
    private String errorMessage;
}
