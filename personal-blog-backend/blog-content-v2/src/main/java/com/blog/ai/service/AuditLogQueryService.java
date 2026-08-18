package com.blog.ai.service;

public interface AuditLogQueryService {
    void record(String user, String action, String detail, String ip);
}
