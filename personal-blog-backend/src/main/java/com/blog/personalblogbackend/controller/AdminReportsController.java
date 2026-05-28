package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.common.support.PageResult;
import com.blog.personalblogbackend.common.support.Result;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.model.dto.report.StoredReportListItemDto;
import com.blog.personalblogbackend.model.entity.StoredReport;
import com.blog.personalblogbackend.service.ReportStorageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportsController {

    private final ReportStorageService reportStorageService;

    @GetMapping("/weekly")
    public Result<PageResult<StoredReportListItemDto>> weekly(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        IPage<StoredReport> raw = reportStorageService.pageWeekly(new Page<>(page, size));
        return Result.success(toPage(raw));
    }

    @GetMapping("/freshness")
    public Result<PageResult<StoredReportListItemDto>> freshness(
            @RequestParam(required = false) Long articleId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        IPage<StoredReport> raw = reportStorageService.pageFreshness(new Page<>(page, size), articleId);
        return Result.success(toPage(raw));
    }

    @GetMapping("/{id}")
    public Result<String> content(@PathVariable Long id) {
        StoredReport report = reportStorageService.findById(id);
        if (report == null) {
            throw new ServiceException(404, "报告不存在");
        }
        return Result.success(reportStorageService.loadMarkdown(report));
    }

    private PageResult<StoredReportListItemDto> toPage(IPage<StoredReport> raw) {
        return new PageResult<>(
                raw.getRecords().stream().map(this::toItem).toList(),
                raw.getTotal(),
                raw.getCurrent(),
                raw.getSize());
    }

    private StoredReportListItemDto toItem(StoredReport r) {
        StoredReportListItemDto dto = new StoredReportListItemDto();
        dto.setId(r.getId());
        dto.setReportType(r.getReportType());
        dto.setTargetId(r.getTargetId());
        dto.setTitle(r.getTitle());
        dto.setFileSize(r.getFileSize());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}
