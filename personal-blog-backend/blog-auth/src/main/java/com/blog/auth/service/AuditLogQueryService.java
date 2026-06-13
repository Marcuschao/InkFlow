package com.blog.auth.service;

public interface AuditLogQueryService {
    void record(String user, String action, String detail, String ip);
}
