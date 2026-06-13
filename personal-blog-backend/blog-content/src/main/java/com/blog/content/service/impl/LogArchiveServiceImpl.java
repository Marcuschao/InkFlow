package com.blog.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.mapper.AuditLogMapper;
import com.blog.content.model.entity.AuditLog;
import com.blog.content.model.vo.audit.AuditLogVo;
import com.blog.content.monitor.SlowApiMonitorAspect;
import com.blog.content.service.LogArchiveService;
import com.blog.content.service.MinioStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogArchiveServiceImpl implements LogArchiveService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int BATCH_SIZE = 1000;
    private static final int RETENTION_DAYS = 90;

    private final AuditLogMapper auditLogMapper;
    private final ObjectProvider<MinioStorageService> minioStorageProvider;
    private final ObjectMapper objectMapper;
    private final SlowApiMonitorAspect slowApiMonitorAspect;

    @Override
    @Transactional
    public void archiveAuditLogs() {
        MinioStorageService minio = minioStorageProvider.getIfAvailable();
        if (minio == null) {
            log.debug("MinIO 未启用，跳过 audit_log 归档");
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now(ZONE).minusDays(RETENTION_DAYS);
        int total = 0;
        int shard = 0;
        while (true) {
            List<AuditLog> rows = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
                    .lt(AuditLog::getCreatedAt, cutoff)
                    .orderByAsc(AuditLog::getId)
                    .last("LIMIT " + BATCH_SIZE));
            if (rows.isEmpty()) {
                break;
            }
            LocalDate month = rows.get(0).getCreatedAt().toLocalDate();
            String prefix = "logs/operation/" + month.format(DateTimeFormatter.ofPattern("yyyy/MM")) + "/";
            String objectKey = prefix + "audit_" + System.currentTimeMillis() + "_" + shard + ".json.gz";
            List<AuditLogVo> payload = rows.stream().map(this::toVo).collect(Collectors.toList());
            uploadGzip(minio, minio.bucketLogs(), objectKey, payload);
            List<Long> ids = rows.stream().map(AuditLog::getId).collect(Collectors.toList());
            auditLogMapper.deleteBatchIds(ids);
            total += ids.size();
            shard++;
            if (rows.size() < BATCH_SIZE) {
                break;
            }
        }
        if (total > 0) {
            log.info("audit_log 归档完成 count={}", total);
        }
    }

    @Override
    public void exportSlowApiSnapshot() {
        MinioStorageService minio = minioStorageProvider.getIfAvailable();
        if (minio == null) {
            return;
        }
        List<SlowApiMonitorAspect.SlowApiRecord> records = slowApiMonitorAspect.snapshotAll();
        if (records.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now(ZONE);
        String objectKey = "logs/slow/slow_" + today + ".json.gz";
        uploadGzip(minio, minio.bucketLogs(), objectKey, records);
        log.info("slow-api 快照已上传 count={}", records.size());
    }

    private void uploadGzip(MinioStorageService minio, String bucket, String objectKey, Object payload) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(json);
            }
            minio.putObject(bucket, objectKey, baos.toByteArray(), "application/gzip");
        } catch (Exception e) {
            throw new IllegalStateException("日志归档上传失败: " + objectKey, e);
        }
    }

    private AuditLogVo toVo(AuditLog e) {
        AuditLogVo v = new AuditLogVo();
        v.setId(e.getId());
        v.setUsername(e.getUsername());
        v.setAction(e.getAction());
        v.setDetail(e.getDetail());
        v.setIp(e.getIp());
        v.setCreatedAt(e.getCreatedAt());
        return v;
    }
}
