package com.blog.personalblogbackend.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.mapper.AuditLogMapper;
import com.blog.personalblogbackend.model.entity.AuditLog;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditLogCleanupTask {
    private final AuditLogMapper auditLogMapper;

    public AuditLogCleanupTask(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Scheduled(cron = "0 30 3 * * ?", zone = "Asia/Shanghai")
    public void cleanup() {
        auditLogMapper.delete(new LambdaQueryWrapper<AuditLog>()
                .lt(AuditLog::getCreatedAt, LocalDateTime.now().minusDays(90)));
    }
}
