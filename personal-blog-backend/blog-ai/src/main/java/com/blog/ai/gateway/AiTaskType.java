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
    CHAT;

    public String code() {
        return name().toLowerCase();
    }
}
