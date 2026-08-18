package com.blog.ai.runtime.tool;
import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.runtime.model.AgentErrorCode;
import com.blog.ai.runtime.model.AgentExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
@Component
public class ToolPolicyEngine {
    public void authorize(ToolDefinition d, ToolInvocation i, AgentExecutionContext c) {
        if (d.idempotencyRequired() && !StringUtils.hasText(i.getIdempotencyKey())) throw denied("Write tool requires an idempotency key");
        if (d.userConfirmationRequired() && !i.isUserConfirmed()) throw denied("Tool requires user confirmation");
        if (d.adminApprovalRequired() && !i.isAdminApproved()) throw denied("Tool requires administrator approval");
        if (d.adminApprovalRequired() && !c.permissions().contains("ROLE_ADMIN") && !c.permissions().contains("ADMIN"))
            throw denied("Administrator approval must be verified by the server-side principal");
        if (d.riskLevel() != ToolRiskLevel.READ_ONLY && c.userId() == null) throw denied("Guest principals cannot invoke write tools");
        if (!c.permissions().containsAll(d.requiredPermissions())) throw denied("Principal lacks required tool permissions");
    }
    private ServiceException denied(String m) { return new ServiceException(403, AgentErrorCode.AGENT_TOOL_DENIED.name() + ": " + m); }
}
