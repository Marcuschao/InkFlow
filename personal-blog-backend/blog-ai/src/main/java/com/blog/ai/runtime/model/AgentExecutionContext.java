package com.blog.ai.runtime.model;

import com.blog.ai.rag.session.ChatPrincipal;
import java.util.Set;

public record AgentExecutionContext(
        Long userId,
        String guestIdHash,
        String tenantId,
        String username,
        ChatPrincipal chatPrincipal,
        Set<String> permissions,
        String runId,
        String traceId
) {
    public AgentExecutionContext(Long userId, String guestIdHash, String tenantId,
                                 String username, ChatPrincipal chatPrincipal) {
        this(userId, guestIdHash, tenantId, username, chatPrincipal, Set.of(), null, null);
    }
    public AgentExecutionContext(Long userId, String guestIdHash, String tenantId, String username,
                                 ChatPrincipal chatPrincipal, Set<String> permissions) {
        this(userId, guestIdHash, tenantId, username, chatPrincipal, permissions, null, null);
    }
    public AgentExecutionContext withRun(String runId, String traceId) {
        return new AgentExecutionContext(userId, guestIdHash, tenantId, username, chatPrincipal,
                permissions, runId, traceId);
    }
    public boolean owns(AgentRun run) {
        if (userId != null) return userId.equals(run.getUserId());
        return guestIdHash != null && guestIdHash.equals(run.getGuestIdHash());
    }
}
