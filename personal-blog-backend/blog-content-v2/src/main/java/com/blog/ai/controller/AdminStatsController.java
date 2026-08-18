package com.blog.ai.controller;

import com.blog.ai.common.support.Result;
import com.blog.ai.model.dto.stat.AiUsageItemDto;
import com.blog.ai.model.dto.stat.PvTrendDto;
import com.blog.ai.model.dto.stat.StatsSummaryDto;
import com.blog.ai.model.dto.stat.TopArticleStatDto;
import com.blog.ai.service.AdminStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    @Autowired
    private AdminStatsService adminStatsService;

    @GetMapping("/summary")
    public Result<StatsSummaryDto> summary() {
        return Result.success(adminStatsService.summary());
    }

    @GetMapping("/pv-trend")
    public Result<PvTrendDto> pvTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(adminStatsService.pvTrend(days));
    }

    @GetMapping("/top-articles")
    public Result<List<TopArticleStatDto>> topArticles(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(adminStatsService.topArticles(limit));
    }

    @GetMapping("/ai-usage")
    public Result<List<AiUsageItemDto>> aiUsage(@RequestParam(defaultValue = "week") String period) {
        return Result.success(adminStatsService.aiUsage(period));
    }
}
