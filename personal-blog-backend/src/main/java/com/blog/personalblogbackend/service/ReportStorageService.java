package com.blog.personalblogbackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.personalblogbackend.model.entity.StoredReport;

public interface ReportStorageService {

    StoredReport saveWeeklyReport(String title, String markdown);

    StoredReport saveFreshnessReport(Long articleId, String title, String markdown);

    String loadMarkdown(StoredReport report);

    IPage<StoredReport> pageWeekly(Page<StoredReport> page);

    IPage<StoredReport> pageFreshness(Page<StoredReport> page, Long articleId);

    StoredReport findById(Long id);
}
