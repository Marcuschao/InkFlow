package com.blog.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.ai.mapper.StoredReportMapper;
import com.blog.ai.model.entity.StoredReport;
import com.blog.ai.service.MinioStorageService;
import com.blog.ai.service.ReportStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportStorageServiceImpl implements ReportStorageService {

    public static final String TYPE_WEEKLY = "WEEKLY";
    public static final String TYPE_FRESHNESS = "FRESHNESS";

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final StoredReportMapper storedReportMapper;
    private final ObjectProvider<MinioStorageService> minioStorageProvider;

    @Override
    public StoredReport saveWeeklyReport(String title, String markdown) {
        LocalDate now = LocalDate.now(ZONE);
        String objectKey = "reports/weekly/" + now.format(MONTH_FMT) + "/"
                + UUID.randomUUID().toString().replace("-", "") + ".md";
        return save(TYPE_WEEKLY, null, title, objectKey, markdown);
    }

    @Override
    public StoredReport saveFreshnessReport(Long articleId, String title, String markdown) {
        String objectKey = "reports/freshness/" + articleId + "/"
                + UUID.randomUUID().toString().replace("-", "") + ".md";
        return save(TYPE_FRESHNESS, articleId, title, objectKey, markdown);
    }

    @Override
    public String loadMarkdown(StoredReport report) {
        if (report == null || !StringUtils.hasText(report.getObjectKey())) {
            return "";
        }
        MinioStorageService minio = minioStorageProvider.getIfAvailable();
        if (minio == null) {
            return "";
        }
        try (InputStream in = minio.getObject(minio.bucketReports(), report.getObjectKey())) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public IPage<StoredReport> pageWeekly(Page<StoredReport> page) {
        return storedReportMapper.selectPage(page, new LambdaQueryWrapper<StoredReport>()
                .eq(StoredReport::getReportType, TYPE_WEEKLY)
                .orderByDesc(StoredReport::getCreatedAt));
    }

    @Override
    public IPage<StoredReport> pageFreshness(Page<StoredReport> page, Long articleId) {
        LambdaQueryWrapper<StoredReport> qw = new LambdaQueryWrapper<StoredReport>()
                .eq(StoredReport::getReportType, TYPE_FRESHNESS)
                .orderByDesc(StoredReport::getCreatedAt);
        if (articleId != null) {
            qw.eq(StoredReport::getTargetId, articleId);
        }
        return storedReportMapper.selectPage(page, qw);
    }

    @Override
    public StoredReport findById(Long id) {
        return storedReportMapper.selectById(id);
    }

    private StoredReport save(String type, Long targetId, String title, String objectKey, String markdown) {
        byte[] bytes = (markdown == null ? "" : markdown).getBytes(StandardCharsets.UTF_8);
        MinioStorageService minio = minioStorageProvider.getIfAvailable();
        if (minio != null) {
            minio.putObject(minio.bucketReports(), objectKey, bytes, "text/markdown; charset=utf-8");
        }
        StoredReport row = new StoredReport();
        row.setReportType(type);
        row.setTargetId(targetId);
        row.setObjectKey(objectKey);
        row.setTitle(StringUtils.hasText(title) ? title : "报告");
        row.setFileSize((long) bytes.length);
        row.setCreatedAt(LocalDateTime.now(ZONE));
        storedReportMapper.insert(row);
        return row;
    }
}
