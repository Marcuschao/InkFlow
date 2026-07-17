package com.blog.ai.controller;

import com.blog.ai.common.support.PageResult;
import com.blog.ai.common.support.Result;
import com.blog.ai.model.dto.ai.AiModelCreateRequest;
import com.blog.ai.model.dto.ai.AiModelHealthDto;
import com.blog.ai.model.dto.ai.AiModelUsageDto;
import com.blog.ai.model.dto.ai.AiQuotaDto;
import com.blog.ai.model.dto.ai.AiStatsOverviewDto;
import com.blog.ai.model.dto.ai.AiStatsTrendDto;
import com.blog.ai.model.dto.ai.AiUserUsageDto;
import com.blog.ai.model.entity.AiCallLog;
import com.blog.ai.model.entity.AiGuardRule;
import com.blog.ai.model.entity.AiModelConfig;
import com.blog.ai.model.entity.AiQuotaWhitelist;
import com.blog.ai.service.AdminAiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai")
public class AdminAiController {

    private final AdminAiService adminAiService;

    public AdminAiController(AdminAiService adminAiService) {
        this.adminAiService = adminAiService;
    }

    @GetMapping("/stats/overview")
    public Result<AiStatsOverviewDto> overview() {
        return Result.success(adminAiService.overview());
    }

    @GetMapping("/stats/trend")
    public Result<AiStatsTrendDto> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(adminAiService.trend(days));
    }

    @GetMapping("/stats/by-model")
    public Result<List<AiModelUsageDto>> byModel() {
        return Result.success(adminAiService.byModel());
    }

    @GetMapping("/stats/by-user")
    public Result<List<AiUserUsageDto>> byUser(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(adminAiService.byUser(limit));
    }

    @GetMapping("/logs")
    public Result<PageResult<AiCallLog>> logs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(adminAiService.logs(page, size));
    }

    @GetMapping("/models")
    public Result<List<Map<String, Object>>> models() {
        return Result.success(adminAiService.listModels());
    }

    @PutMapping("/models/{id}/enabled")
    public Result<Void> updateModelEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        adminAiService.updateModelEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
        return Result.success(null);
    }

    @PostMapping("/models")
    public Result<AiModelConfig> addModel(@Valid @RequestBody AiModelCreateRequest req) {
        return Result.success(adminAiService.addModel(req));
    }

    @DeleteMapping("/models/{id}")
    public Result<Void> deleteModel(@PathVariable Long id) {
        adminAiService.deleteModel(id);
        return Result.success(null);
    }

    @GetMapping("/models/health")
    public Result<List<AiModelHealthDto>> modelHealth() {
        return Result.success(adminAiService.modelHealth());
    }

    @GetMapping("/quota")
    public Result<AiQuotaDto> quota() {
        return Result.success(adminAiService.getQuota());
    }

    @PutMapping("/quota")
    public Result<Void> saveQuota(@RequestBody Map<String, Long> body) {
        adminAiService.saveQuota(body.getOrDefault("globalDailyTokens", 1_000_000L),
                body.getOrDefault("userDailyTokens", 50_000L));
        return Result.success(null);
    }

    @GetMapping("/quota/whitelist")
    public Result<List<AiQuotaWhitelist>> whitelist() {
        return Result.success(adminAiService.listWhitelist());
    }

    @PostMapping("/quota/whitelist")
    public Result<Void> addWhitelist(@RequestBody Map<String, Object> body) {
        Object uid = body.get("userId");
        Long userId = uid instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(uid));
        adminAiService.addWhitelist(userId, body.get("remark") != null ? String.valueOf(body.get("remark")) : null);
        return Result.success(null);
    }

    @DeleteMapping("/quota/whitelist/{id}")
    public Result<Void> removeWhitelist(@PathVariable Long id) {
        adminAiService.removeWhitelist(id);
        return Result.success(null);
    }

    @GetMapping("/guard/rules")
    public Result<List<AiGuardRule>> guardRules() {
        return Result.success(adminAiService.listGuardRules());
    }

    @PostMapping("/guard/rules")
    public Result<AiGuardRule> addGuardRule(@RequestBody AiGuardRule rule) {
        return Result.success(adminAiService.addGuardRule(rule));
    }

    @PutMapping("/guard/rules/{id}")
    public Result<Void> updateGuardRule(@PathVariable Long id, @RequestBody AiGuardRule rule) {
        rule.setId(id);
        adminAiService.updateGuardRule(rule);
        return Result.success(null);
    }

    @DeleteMapping("/guard/rules/{id}")
    public Result<Void> deleteGuardRule(@PathVariable Long id) {
        adminAiService.deleteGuardRule(id);
        return Result.success(null);
    }
}
