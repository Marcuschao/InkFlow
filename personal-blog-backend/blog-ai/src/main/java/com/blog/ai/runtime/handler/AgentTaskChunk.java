package com.blog.ai.runtime.handler;

import com.blog.ai.runtime.model.AgentEventType;
import com.blog.ai.runtime.model.AgentResult;

import java.util.Map;

public record AgentTaskChunk(AgentEventType type, Map<String, Object> data, AgentResult result) {
    public static AgentTaskChunk event(AgentEventType type, Map<String, Object> data) { return new AgentTaskChunk(type, data, null); }
    public static AgentTaskChunk result(AgentResult result) { return new AgentTaskChunk(null, Map.of(), result); }
}
