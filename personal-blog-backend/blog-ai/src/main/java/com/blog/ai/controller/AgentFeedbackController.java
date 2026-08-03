package com.blog.ai.controller;
import com.blog.ai.common.support.Result;
import com.blog.ai.config.security.CurrentUserService;
import com.blog.ai.model.dto.eval.FeedbackRequest;
import com.blog.ai.model.entity.AiAnswerFeedback;
import com.blog.ai.service.EvalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/agent/feedback")
public class AgentFeedbackController {
    private final EvalService service; private final CurrentUserService currentUser;
    public AgentFeedbackController(EvalService service,CurrentUserService currentUser){this.service=service;this.currentUser=currentUser;}
    @PostMapping public Result<AiAnswerFeedback> save(@Valid @RequestBody FeedbackRequest r){return Result.success(service.feedback(currentUser.requireUserId(),r));}
    @GetMapping public Result<AiAnswerFeedback> get(@RequestParam Long messageId){return Result.success(service.feedback(currentUser.requireUserId(),messageId));}
}
