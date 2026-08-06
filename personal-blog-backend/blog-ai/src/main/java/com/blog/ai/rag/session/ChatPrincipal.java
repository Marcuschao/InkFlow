package com.blog.ai.rag.session;

public record ChatPrincipal(Long userId, String guestTokenHash) {
    public boolean authenticated() {
        return userId != null;
    }
}
