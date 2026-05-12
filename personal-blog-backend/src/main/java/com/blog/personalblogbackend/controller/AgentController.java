package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.audit.Audit;
import com.blog.personalblogbackend.common.Result;
import com.blog.personalblogbackend.dto.agent.*;
import com.blog.personalblogbackend.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @PostMapping("/outline")
    public Result<String> outline(@RequestBody OutlineRequest request) {
        return Result.success(agentService.outline(request));
    }

    @PostMapping("/expand")
    public Result<String> expand(@RequestBody ExpandRequest request) {
        return Result.success(agentService.expand(request));
    }

    @PostMapping("/polish")
    public Result<String> polish(@RequestBody PolishRequest request) {
        return Result.success(agentService.polish(request));
    }

    @PostMapping("/editor/outline")
    public Result<String> editorOutline(@RequestBody EditorOutlineRequest request) {
        return Result.success(agentService.editorOutline(request));
    }

    @PostMapping("/editor/continue")
    public Result<String> editorContinue(@RequestBody EditorContinueRequest request) {
        return Result.success(agentService.editorContinue(request));
    }

    @PostMapping("/editor/polish")
    public Result<String> editorPolish(@RequestBody EditorPolishRequest request) {
        return Result.success(agentService.editorPolish(request));
    }

    @PostMapping("/summary")
    public Result<String> summary(@RequestBody SummaryRequest request) {
        return Result.success(agentService.summary(request));
    }

    @PostMapping("/tags")
    public Result<List<String>> tags(@RequestBody TagsRequest request) {
        return Result.success(agentService.tags(request));
    }

    @Audit("AGENT_CHAT")
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        return Result.success(agentService.chat(request));
    }

    @Audit("AGENT_WEEKLY")
    @PostMapping("/weekly-report")
    public Result<String> weeklyReport(@RequestBody(required = false) WeeklyReportRequest request) {
        if (request == null) {
            request = new WeeklyReportRequest();
        }
        return Result.success(agentService.weeklyReport(request));
    }

    @GetMapping("/recommend")
    public Result<List<RecommendArticleDto>> recommend(@RequestParam Long articleId) {
        return Result.success(agentService.recommend(articleId));
    }

    @PostMapping("/recommend/context")
    public Result<List<RecommendArticleDto>> recommendContext(@RequestBody RecommendContextRequest request) {
        if (request == null || request.getArticleId() == null) {
            return Result.fail(400, "articleId 不能为空");
        }
        return Result.success(agentService.recommendWithContext(request.getArticleId(), request.getRecentArticleIds()));
    }

    @PostMapping("/recommend/home")
    public Result<List<RecommendArticleDto>> recommendHome(@RequestBody(required = false) RecommendHomeRequest request) {
        List<Long> ids = request != null ? request.getRecentArticleIds() : null;
        return Result.success(agentService.recommendHome(ids));
    }
}
