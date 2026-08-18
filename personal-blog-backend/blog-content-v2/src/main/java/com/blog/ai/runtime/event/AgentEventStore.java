package com.blog.ai.runtime.event;

import com.blog.ai.runtime.model.AgentEvent;

import java.util.List;

public interface AgentEventStore {
    void append(AgentEvent event);
    List<AgentEvent> replay(String runId, long afterSequence, int limit);
}
