package com.blog.content.controller;

import com.blog.content.common.support.PageResult;
import com.blog.content.common.support.Result;
import com.blog.content.model.vo.audit.AuditLogVo;
import com.blog.content.service.AuditLogQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminAuditLogController {

    @Autowired
    private AuditLogQueryService auditLogQueryService;

    @GetMapping("/logs")
    public Result<PageResult<AuditLogVo>> logs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        long sz = Math.min(Math.max(size, 1), 100);
        long pg = Math.max(page, 1);
        return Result.success(auditLogQueryService.page(pg, sz));
    }
}
