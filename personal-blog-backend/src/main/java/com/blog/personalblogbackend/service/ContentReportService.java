package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.common.support.PageResult;
import com.blog.personalblogbackend.model.entity.ContentReport;

public interface ContentReportService {

    void reportArticle(Long articleId, Long reporterId, String reason);

    PageResult<ContentReport> adminPage(int page, int size, Integer status);

    void handle(Long reportId, Long handlerId, int status, String note);
}
