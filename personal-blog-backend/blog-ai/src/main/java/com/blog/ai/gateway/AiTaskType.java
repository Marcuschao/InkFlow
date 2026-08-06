package com.blog.ai.gateway;

public enum AiTaskType {
    GENERIC,
    WRITING,
    TAG,
    AGENT,
    TRANSLATE,
    SEO,
    FRESHNESS,
    RAG,
    GUARD,
    CHAT;

    public String code() {
        return name().toLowerCase();
    }
}
