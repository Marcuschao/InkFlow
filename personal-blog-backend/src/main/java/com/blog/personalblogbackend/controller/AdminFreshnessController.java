package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.audit.Audit;
import com.blog.personalblogbackend.common.PageResult;
import com.blog.personalblogbackend.common.Result;
import com.blog.personalblogbackend.dto.freshness.FreshnessAiDraftDto;
import com.blog.personalblogbackend.dto.freshness.FreshnessSummaryDto;
import com.blog.personalblogbackend.entity.Article;
import com.blog.personalblogbackend.service.ArticleService;
import com.blog.personalblogbackend.service.FreshnessService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/freshness")
public class AdminFreshnessController {

    @Autowired
    private FreshnessService freshnessService;
    @Autowired
    private ArticleService articleService;

    @GetMapping("/summary")
    public Result<FreshnessSummaryDto> summary() {
        return Result.success(freshnessService.summary());
    }

    @GetMapping("/articles")
    public Result<PageResult<Article>> articles(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) Integer freshnessStatus) {
        Page<Article> p = new Page<>(page, size);
        IPage<Article> data = freshnessService.pagePublishedByFreshness(p, freshnessStatus);
        return Result.success(PageResult.build(data.getRecords(), data.getTotal(), data.getSize(), data.getCurrent()));
    }

    @Audit("FRESHNESS_AI_REFRESH")
    @PostMapping("/{id}/ai-refresh")
    public Result<FreshnessAiDraftDto> aiRefresh(@PathVariable Long id) {
        return Result.success(freshnessService.aiRefreshDraft(id));
    }

    @Audit("FRESHNESS_FORK_DRAFT")
    @PostMapping("/{id}/fork-draft")
    public Result<Long> forkDraft(@PathVariable Long id) {
        return Result.success(articleService.duplicateArticleAsDraft(id));
    }

    @Audit("FRESHNESS_SCAN_MANUAL")
    @PostMapping("/scan-run")
    public Result<Void> scanRun() {
        freshnessService.runFullScan();
        return Result.success(null);
    }
}
