package com.blog.ai.runtime.model;

public enum AgentEventType {
    RUN_STARTED("run.started"),
    STATE_CHANGED("run.state"),
    RUN_CLASSIFIED("run.classified"),
    RETRIEVAL_STARTED("retrieval.started"),
    RETRIEVAL_COMPLETED("retrieval.completed"),
    TOOL_STARTED("tool.started"),
    TOOL_COMPLETED("tool.completed"),
    DELTA("delta"),
    CITATION("citation"),
    VALIDATION_COMPLETED("validation.completed"),
    SAFETY_BLOCKED("safety.blocked"),
    RUN_COMPLETED("run.completed"),
    RUN_FAILED("run.failed"),
    RUN_CANCELLED("run.cancelled"),
    HEARTBEAT("heartbeat"),
    ERROR("error"),
    DEGRADED("degraded");

    private final String wireName;

    AgentEventType(String wireName) { this.wireName = wireName; }
    public String wireName() { return wireName; }
}
