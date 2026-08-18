package com.blog.content.service;

import com.blog.content.common.support.PageResult;
import com.blog.content.model.entity.ContentReport;

public interface ContentReportService {

    void reportArticle(Long articleId, Long reporterId, String reason);

    PageResult<ContentReport> adminPage(int page, int size, Integer status);

    void handle(Long reportId, Long handlerId, int status, String note);
}
