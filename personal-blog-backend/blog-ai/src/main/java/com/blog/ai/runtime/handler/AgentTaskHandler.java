package com.blog.ai.runtime.handler;

import com.blog.ai.runtime.model.AgentExecutionContext;
import com.blog.ai.runtime.model.AgentRequest;
import com.blog.ai.runtime.model.AgentResult;
import com.blog.ai.runtime.model.AgentTaskType;
import reactor.core.publisher.Flux;

public interface AgentTaskHandler {
    AgentTaskType supports();
    AgentResult execute(AgentRequest request, AgentExecutionContext context);
    default Flux<AgentTaskChunk> stream(AgentRequest request, AgentExecutionContext context) {
        return Flux.defer(() -> {
            AgentResult result = execute(request, context);
            result.setDegraded(true);
            return Flux.just(
                    AgentTaskChunk.event(com.blog.ai.runtime.model.AgentEventType.DEGRADED,
                            java.util.Map.of("reason", "TASK_DOES_NOT_SUPPORT_NATIVE_STREAMING")),
                    AgentTaskChunk.event(com.blog.ai.runtime.model.AgentEventType.DELTA,
                            java.util.Map.of("delta", result.getAnswer() == null ? "" : result.getAnswer())),
                    AgentTaskChunk.result(result));
        });
    }
}
