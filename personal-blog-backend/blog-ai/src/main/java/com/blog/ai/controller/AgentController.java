package com.blog.ai.controller;

import com.blog.ai.config.audit.Audit;
import com.blog.ai.common.support.Result;
import com.blog.ai.model.dto.agent.*;
import com.blog.ai.service.AgentService;
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

    @PostMapping("/auto-tag")
    public Result<List<com.blog.common.dto.AutoTagItemDto>> autoTag(@RequestBody com.blog.common.dto.AutoTagRequest request) {
        return Result.success(agentService.autoTag(request));
    }

    @PostMapping("/learning-path")
    public Result<com.blog.common.dto.LearningPathResult> learningPath(@RequestBody com.blog.common.dto.LearningPathRequest request) {
        return Result.success(agentService.learningPath(request));
    }

    @GetMapping("/weekly-insight")
    public Result<String> weeklyInsight() {
        return Result.success(agentService.weeklyInsight());
    }

    /**
     * 博客问答接口，多Agent协同，基于 Langchain4j 实现 Agent 间的工具调用和对话管理
     *
     * @param request
     * @return
     */
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
