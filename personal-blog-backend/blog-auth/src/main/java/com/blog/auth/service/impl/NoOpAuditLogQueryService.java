package com.blog.auth.service.impl;

import com.blog.auth.mapper.AuditLogMapper;
import com.blog.auth.model.entity.AuditLog;
import com.blog.auth.service.AuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NoOpAuditLogQueryService implements AuditLogQueryService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public void record(String username, String action, String detail, String ip) {
        AuditLog row = new AuditLog();
        row.setUsername(username != null ? username : "");
        row.setAction(action);
        row.setDetail(detail);
        row.setIp(ip);
        row.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(row);
    }
}
