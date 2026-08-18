package com.blog.ai.runtime.model;

import java.util.Set;

public enum AgentRunStatus {
    START, CLASSIFY, RETRIEVE, PLAN, TOOL_CALL, VALIDATE, GENERATE, SAFETY_CHECK,
    COMPLETED, FAILED, CANCELLED, TIMEOUT, BUDGET_EXCEEDED, SAFETY_BLOCKED;

    private static final Set<AgentRunStatus> TERMINAL = Set.of(
            COMPLETED, FAILED, CANCELLED, TIMEOUT, BUDGET_EXCEEDED, SAFETY_BLOCKED);

    public boolean terminal() {
        return TERMINAL.contains(this);
    }
}
