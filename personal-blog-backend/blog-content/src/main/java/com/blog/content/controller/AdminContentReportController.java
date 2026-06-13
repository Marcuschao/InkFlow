package com.blog.content.controller;

import com.blog.content.common.support.PageResult;
import com.blog.content.common.support.Result;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.model.dto.ContentReportRequest;
import com.blog.content.model.entity.ContentReport;
import com.blog.content.service.ContentReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/content-reports")
public class AdminContentReportController {

    private final ContentReportService contentReportService;
    private final CurrentUserService currentUserService;

    public AdminContentReportController(ContentReportService contentReportService,
                                        CurrentUserService currentUserService) {
        this.contentReportService = contentReportService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Result<PageResult<ContentReport>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status) {
        return Result.success(contentReportService.adminPage(page, size, status));
    }

    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody ContentReportRequest body) {
        int status = body != null && body.getStatus() != null
                ? body.getStatus()
                : ContentReport.STATUS_HANDLED;
        contentReportService.handle(id, currentUserService.requireUserId(), status, body != null ? body.getNote() : null);
        return Result.success(null);
    }
}
