package com.blog.ai.runtime.core;

import com.blog.ai.runtime.model.AgentRunStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class AgentStateMachine {
    private final Map<AgentRunStatus, Set<AgentRunStatus>> transitions = new EnumMap<>(AgentRunStatus.class);

    public AgentStateMachine() {
        allow(AgentRunStatus.START, AgentRunStatus.CLASSIFY);
        allow(AgentRunStatus.CLASSIFY, AgentRunStatus.RETRIEVE, AgentRunStatus.PLAN);
        allow(AgentRunStatus.RETRIEVE, AgentRunStatus.PLAN);
        allow(AgentRunStatus.PLAN, AgentRunStatus.TOOL_CALL, AgentRunStatus.VALIDATE, AgentRunStatus.GENERATE);
        allow(AgentRunStatus.TOOL_CALL, AgentRunStatus.VALIDATE, AgentRunStatus.GENERATE);
        allow(AgentRunStatus.VALIDATE, AgentRunStatus.GENERATE, AgentRunStatus.SAFETY_CHECK);
        allow(AgentRunStatus.GENERATE, AgentRunStatus.SAFETY_CHECK);
        allow(AgentRunStatus.SAFETY_CHECK, AgentRunStatus.COMPLETED, AgentRunStatus.SAFETY_BLOCKED);
    }

    private void allow(AgentRunStatus from, AgentRunStatus... to) {
        transitions.put(from, EnumSet.copyOf(java.util.List.of(to)));
    }

    public void assertTransition(AgentRunStatus from, AgentRunStatus to) {
        if (to == AgentRunStatus.FAILED || to == AgentRunStatus.CANCELLED ||
                to == AgentRunStatus.TIMEOUT || to == AgentRunStatus.BUDGET_EXCEEDED) {
            if (!from.terminal()) return;
        }
        if (!transitions.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStateException("Illegal agent state transition: " + from + " -> " + to);
        }
    }
}
