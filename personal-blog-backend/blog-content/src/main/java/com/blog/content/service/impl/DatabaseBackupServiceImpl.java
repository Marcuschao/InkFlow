package com.blog.content.service.impl;

import com.blog.content.common.constant.BlogSiteKeys;
import com.blog.content.common.exception.ServiceException;
import com.blog.content.service.DatabaseBackupService;
import com.blog.content.service.MinioStorageService;
import com.blog.content.service.SiteKvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseBackupServiceImpl implements DatabaseBackupService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern JDBC_URL = Pattern.compile("jdbc:mysql://([^:/]+)(?::(\\d+))?/([^?]+)");
    private static final int RETENTION_DAYS = 30;

    private final ObjectProvider<MinioStorageService> minioStorageProvider;
    private final SiteKvService siteKvService;
    private final DataSource dataSource;

    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    @Override
    public void runScheduledBackup() {
        try {
            triggerBackup();
        } catch (Exception e) {
            log.error("定时数据库备份失败", e);
        }
    }

    @Override
    public void triggerBackup() {
        MinioStorageService minio = minioStorageProvider.getIfAvailable();
        if (minio == null) {
            throw new ServiceException(503, "MinIO 未启用，无法备份");
        }
        DbConn conn = parseJdbc(jdbcUrl);
        byte[] sqlBytes = JdbcSqlBackupExporter.export(dataSource, conn.database());
        byte[] gz;
        try {
            gz = gzip(sqlBytes);
        } catch (Exception e) {
            throw new ServiceException(500, "压缩备份失败: " + e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now(ZONE);
        String objectKey = "backups/database/"
                + now.format(DateTimeFormatter.ofPattern("yyyy/MM"))
                + "/blog_db_" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql.gz";
        minio.putObject(minio.bucketBackups(), objectKey, gz, "application/gzip");

        siteKvService.put(BlogSiteKeys.BACKUP_LAST_TIME, now.toString());
        siteKvService.put(BlogSiteKeys.BACKUP_LAST_SIZE, String.valueOf(gz.length));
        siteKvService.put(BlogSiteKeys.BACKUP_LAST_OBJECT_KEY, objectKey);

        purgeOldBackups(minio);
        log.info("数据库备份完成 key={} size={}", objectKey, gz.length);
    }

    @Override
    public BackupStatus getStatus() {
        return new BackupStatus(
                siteKvService.get(BlogSiteKeys.BACKUP_LAST_TIME).orElse(null),
                siteKvService.get(BlogSiteKeys.BACKUP_LAST_SIZE).map(Long::parseLong).orElse(null),
                siteKvService.get(BlogSiteKeys.BACKUP_LAST_OBJECT_KEY).orElse(null),
                null);
    }

    private void purgeOldBackups(MinioStorageService minio) {
        LocalDate cutoff = LocalDate.now(ZONE).minusDays(RETENTION_DAYS);
        List<String> keys = minio.listObjectKeys(minio.bucketBackups(), "backups/database/", 5000);
        for (String key : keys) {
            if (!key.endsWith(".sql.gz")) {
                continue;
            }
            try {
                String part = key.substring(key.lastIndexOf("blog_db_") + "blog_db_".length(), key.length() - ".sql.gz".length());
                LocalDateTime ts = LocalDateTime.parse(part, DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                if (ts.toLocalDate().isBefore(cutoff)) {
                    minio.deleteObject(minio.bucketBackups(), key);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static DbConn parseJdbc(String url) {
        Matcher m = JDBC_URL.matcher(url == null ? "" : url);
        if (!m.find()) {
            throw new ServiceException(500, "无法解析数据库连接 URL");
        }
        return new DbConn(m.group(1), m.group(2), m.group(3));
    }

    private static byte[] gzip(byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
        }
        return baos.toByteArray();
    }

    private record DbConn(String host, String port, String database) {
    }
}
