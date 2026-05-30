package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.common.support.Result;
import com.blog.personalblogbackend.config.security.CurrentUserService;
import com.blog.personalblogbackend.model.dto.ContentReportRequest;
import com.blog.personalblogbackend.service.ContentReportService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class ContentReportController {

    private final ContentReportService contentReportService;
    private final CurrentUserService currentUserService;

    public ContentReportController(ContentReportService contentReportService,
                                   CurrentUserService currentUserService) {
        this.contentReportService = contentReportService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/{id}/report")
    public Result<Void> reportArticle(@PathVariable Long id, @RequestBody ContentReportRequest body) {
        String reason = body != null ? body.getReason() : null;
        contentReportService.reportArticle(id, currentUserService.requireUserId(), reason);
        return Result.success(null);
    }
}
