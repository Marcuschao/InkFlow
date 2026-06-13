package com.blog.auth.service.impl;

import com.blog.auth.service.AuditLogQueryService;
import org.springframework.stereotype.Service;

@Service
public class NoOpAuditLogQueryService implements AuditLogQueryService {
    @Override
    public void record(String user, String action, String detail, String ip) {
    }
}
