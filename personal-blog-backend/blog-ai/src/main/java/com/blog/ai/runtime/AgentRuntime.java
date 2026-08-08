package com.blog.ai.runtime;

import com.blog.ai.runtime.model.*;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AgentRuntime {
    AgentResult run(AgentRequest request, AgentExecutionContext context);
    Flux<AgentEvent> stream(AgentRequest request, AgentExecutionContext context);
    void cancel(String runId, AgentExecutionContext context);
    AgentRun getRun(String runId, AgentExecutionContext context);
    List<AgentEvent> events(String runId, long afterSequence, AgentExecutionContext context);
}
