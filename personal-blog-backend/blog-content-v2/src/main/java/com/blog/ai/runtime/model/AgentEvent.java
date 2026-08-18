package com.blog.ai.runtime.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AgentEvent {
    private String eventId;
    private long sequence;
    private String runId;
    private String traceId;
    private AgentEventType type;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
}
