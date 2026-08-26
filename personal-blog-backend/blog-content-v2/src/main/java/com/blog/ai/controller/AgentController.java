package com.blog.ai.controller;

import com.blog.ai.config.audit.Audit;
import com.blog.ai.common.support.PageResult;
import com.blog.ai.common.support.Result;
import com.blog.ai.model.dto.agent.*;
import com.blog.ai.rag.RagChatOrchestrator;
import com.blog.ai.rag.dto.ChatMessageVo;
import com.blog.ai.rag.dto.ChatSessionVo;
import com.blog.ai.rag.session.ChatSessionService;
import com.blog.ai.rag.session.ChatPrincipalResolver;
import com.blog.ai.service.AgentService;
import com.blog.ai.runtime.AgentRuntime;
import com.blog.ai.runtime.model.AgentExecutionContext;
import com.blog.ai.runtime.model.AgentRequest;
import com.blog.ai.runtime.model.AgentTaskType;
import com.blog.ai.gateway.context.GatewayUserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Set;

import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;
    @Autowired(required = false)
    private ChatSessionService chatSessionService;
    @Autowired(required = false)
    private RagChatOrchestrator ragChatOrchestrator;
    @Autowired(required = false)
    private ChatPrincipalResolver chatPrincipalResolver;
    @Autowired
    private AgentRuntime agentRuntime;
    @Autowired
    @Qualifier("agentRuntimeExecutor")
    private AsyncTaskExecutor agentRuntimeExecutor;

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
    public Result<ChatResponse> chat(@RequestBody ChatRequest request,
                                     HttpServletRequest servletRequest,
                                     HttpServletResponse servletResponse) {
        var result = agentRuntime.run(toRuntimeRequest(request), runtimeContext(servletRequest, servletResponse));
        ChatResponse response = new ChatResponse();
        response.setAnswer(result.getAnswer());
        response.setSources(result.getSources());
        response.setSessionId(result.getSessionId());
        response.setMessageId(result.getMessageId());
        response.setGrounded(result.isGrounded());
        response.setConfidence(result.getConfidence());
        response.setRefusalReason(result.getRefusalReason());
        response.setDegraded(result.isDegraded());
        return Result.success(response);
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

    @GetMapping("/sessions")
    public Result<PageResult<ChatSessionVo>> sessions(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "20") long size,
                                                      @RequestParam(required = false) List<Long> ids,
                                                      HttpServletRequest servletRequest,
                                                      HttpServletResponse servletResponse) {
        if (chatSessionService == null) {
            return Result.fail(503, "RAG 未启用");
        }
        var principal = chatPrincipalResolver.resolve(servletRequest, servletResponse);
        var p = chatSessionService.pageSessions(principal, page, size);
        var vos = p.getRecords().stream().map(s -> {
            ChatSessionVo v = new ChatSessionVo();
            v.setId(s.getId());
            v.setUserId(s.getUserId());
            v.setTitle(s.getTitle());
            v.setCreateTime(s.getCreateTime());
            v.setUpdateTime(s.getUpdateTime());
            return v;
        }).toList();
        return Result.success(PageResult.build(vos, p.getTotal(), p.getSize(), p.getCurrent()));
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<List<ChatMessageVo>> messages(@PathVariable Long id,
                                                @RequestParam(required = false) List<Long> ids,
                                                HttpServletRequest servletRequest,
                                                HttpServletResponse servletResponse) {
        if (chatSessionService == null) {
            return Result.fail(503, "RAG 未启用");
        }
        var principal = chatPrincipalResolver.resolve(servletRequest, servletResponse);
        chatSessionService.assertSessionReadable(id, principal);
        var msgs = chatSessionService.listMessages(id);
        return Result.success(msgs.stream().map(m -> {
            ChatMessageVo v = new ChatMessageVo();
            v.setId(m.getId());
            v.setSessionId(m.getSessionId());
            v.setRole(m.getRole());
            v.setContent(m.getContent());
            v.setCreateTime(m.getCreateTime());
            v.setSources(m.getSourcesJson());
            return v;
        }).toList());
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id,
                                      @RequestParam(required = false) List<Long> ids,
                                      HttpServletRequest servletRequest,
                                      HttpServletResponse servletResponse) {
        if (chatSessionService == null) {
            return Result.fail(503, "RAG 未启用");
        }
        var principal = chatPrincipalResolver.resolve(servletRequest, servletResponse);
        chatSessionService.deleteSession(id, principal);
        return Result.success();
    }

    @Audit("AGENT_CHAT_STREAM")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<com.blog.ai.runtime.model.AgentEvent>> runtimeChatStream(
            @RequestBody ChatRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        return agentRuntime.stream(toRuntimeRequest(request), runtimeContext(servletRequest, servletResponse))
                .map(event -> ServerSentEvent.<com.blog.ai.runtime.model.AgentEvent>builder(event)
                        .id(event.getEventId()).event(event.getType().wireName()).build());
    }

    @Deprecated
    public SseEmitter chatStream(@RequestBody ChatRequest request,
                                 HttpServletRequest servletRequest,
                                 HttpServletResponse servletResponse) {
        SseEmitter emitter = new SseEmitter(120_000L);
        if (ragChatOrchestrator == null) {
            try {
                emitter.send(SseEmitter.event().name("error").data("RAG 未启用"));
                emitter.complete();
            } catch (Exception ignored) {
            }
            return emitter;
        }
        String question = request.resolveQuestion();
        var principal = chatPrincipalResolver.resolve(servletRequest, servletResponse);
        agentRuntimeExecutor.execute(() -> {
            try {
                ChatResponse resp = ragChatOrchestrator.chat(question, request.getSessionId(), principal);
                String answer = resp.getAnswer() == null ? "" : resp.getAnswer();
                emitter.send(SseEmitter.event().name("delta").data(answer));
                emitter.send(SseEmitter.event().name("sources").data(resp.getSources()));
                emitter.send(SseEmitter.event().name("session").data(resp.getSessionId()));
                emitter.send(SseEmitter.event().name("message-id").data(resp.getMessageId()));
                emitter.complete();
            } catch (Exception e) {
                log.error("[agent] chat stream failed", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage() != null ? e.getMessage() : "系统繁忙"));
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        });
        return emitter;
    }

    private AgentRequest toRuntimeRequest(ChatRequest request) {
        AgentRequest runtime = new AgentRequest();
        runtime.setTaskType(AgentTaskType.RAG_QA);
        runtime.setQuestion(request.resolveQuestion());
        runtime.setArticleId(request.getArticleId());
        runtime.setSessionId(request.getSessionId());
        return runtime;
    }

    private AgentExecutionContext runtimeContext(HttpServletRequest request, HttpServletResponse response) {
        var principal = chatPrincipalResolver.resolve(request, response);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> permissions = authentication == null ? Set.of() : authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new AgentExecutionContext(principal.userId(), principal.guestTokenHash(), null,
                GatewayUserContext.getUsername(), principal, permissions);
    }
}
