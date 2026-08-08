package com.blog.ai.runtime.tool;

import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.runtime.model.AgentExecutionContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolPolicyEngineTest {
    private final ToolPolicyEngine policy = new ToolPolicyEngine();

    @Test
    void readOnlyToolAllowsGuest() {
        ToolDefinition definition = definition(ToolRiskLevel.READ_ONLY, false, false, false);
        assertDoesNotThrow(() -> policy.authorize(definition, new ToolInvocation(),
                new AgentExecutionContext(null, "guest", null, null, null)));
    }

    @Test
    void writeToolRequiresIdentityIdempotencyAndApproval() {
        ToolDefinition definition = definition(ToolRiskLevel.HIGH_RISK_WRITE, true, true, true);
        ToolInvocation invocation = new ToolInvocation();
        assertThrows(ServiceException.class, () -> policy.authorize(definition, invocation,
                new AgentExecutionContext(null, "guest", null, null, null)));
        invocation.setIdempotencyKey("key"); invocation.setUserConfirmed(true); invocation.setAdminApproved(true);
        assertThrows(ServiceException.class, () -> policy.authorize(definition, invocation,
                new AgentExecutionContext(1L, null, null, "user", null)));
        assertDoesNotThrow(() -> policy.authorize(definition, invocation,
                new AgentExecutionContext(1L, null, null, "admin", null, Set.of("ROLE_ADMIN"))));
    }

    private ToolDefinition definition(ToolRiskLevel risk, boolean idempotent, boolean confirmation, boolean approval) {
        return new ToolDefinition("test", "1", "test", "{}", risk, Duration.ofSeconds(1), 0,
                idempotent, Set.of(), confirmation, approval);
    }
}
