package com.blog.ai.runtime.tool;
import java.time.Duration;
import java.util.Set;
public record ToolDefinition(String name, String version, String description, String parameterSchema,
        ToolRiskLevel riskLevel, Duration timeout, int maxRetries, boolean idempotencyRequired,
        Set<String> requiredPermissions, boolean userConfirmationRequired, boolean adminApprovalRequired) {}
