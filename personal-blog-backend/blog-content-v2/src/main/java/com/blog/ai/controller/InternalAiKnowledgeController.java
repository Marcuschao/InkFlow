package com.blog.ai.controller;

import com.blog.ai.service.AgentService;
import com.blog.common.dto.AutoTagItemDto;
import com.blog.common.dto.AutoTagRequest;
import com.blog.common.dto.LearningPathRequest;
import com.blog.common.dto.LearningPathResult;
import com.blog.ai.common.support.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/ai")
public class InternalAiKnowledgeController {

    @Autowired
    private AgentService agentService;

    @PostMapping("/auto-tag")
    public Result<List<AutoTagItemDto>> autoTag(@RequestBody AutoTagRequest request) {
        return Result.success(agentService.autoTag(request));
    }

    @PostMapping("/learning-path")
    public Result<LearningPathResult> learningPath(@RequestBody LearningPathRequest request) {
        return Result.success(agentService.learningPath(request));
    }

    @GetMapping("/weekly-insight")
    public Result<String> weeklyInsight() {
        return Result.success(agentService.weeklyInsight());
    }
}
