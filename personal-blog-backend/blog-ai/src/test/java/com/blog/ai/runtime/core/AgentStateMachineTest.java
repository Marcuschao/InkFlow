package com.blog.ai.runtime.core;

import com.blog.ai.runtime.model.AgentRunStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentStateMachineTest {
    private final AgentStateMachine machine = new AgentStateMachine();

    @Test
    void acceptsCanonicalRagPath() {
        assertDoesNotThrow(() -> {
            machine.assertTransition(AgentRunStatus.START, AgentRunStatus.CLASSIFY);
            machine.assertTransition(AgentRunStatus.CLASSIFY, AgentRunStatus.RETRIEVE);
            machine.assertTransition(AgentRunStatus.RETRIEVE, AgentRunStatus.PLAN);
            machine.assertTransition(AgentRunStatus.PLAN, AgentRunStatus.GENERATE);
            machine.assertTransition(AgentRunStatus.GENERATE, AgentRunStatus.SAFETY_CHECK);
            machine.assertTransition(AgentRunStatus.SAFETY_CHECK, AgentRunStatus.COMPLETED);
        });
    }

    @Test
    void rejectsSkippedAndTerminalTransitions() {
        assertThrows(IllegalStateException.class,
                () -> machine.assertTransition(AgentRunStatus.START, AgentRunStatus.GENERATE));
        assertThrows(IllegalStateException.class,
                () -> machine.assertTransition(AgentRunStatus.COMPLETED, AgentRunStatus.FAILED));
    }
}
