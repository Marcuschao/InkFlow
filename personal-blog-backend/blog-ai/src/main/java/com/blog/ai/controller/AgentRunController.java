package com.blog.ai.controller;

import com.blog.ai.common.support.Result;
import com.blog.ai.gateway.context.GatewayUserContext;
import com.blog.ai.rag.session.ChatPrincipalResolver;
import com.blog.ai.runtime.AgentRuntime;
import com.blog.ai.runtime.model.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/agent/runs")
@RequiredArgsConstructor
public class AgentRunController {
    private final AgentRuntime runtime;
    private final ChatPrincipalResolver principalResolver;

    @PostMapping
    public Result<AgentResult> run(@Valid @RequestBody AgentRequest request, HttpServletRequest req, HttpServletResponse res) {
        return Result.success(runtime.run(request, context(req, res)));
    }
    @PostMapping(value="/stream", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentEvent>> stream(@Valid @RequestBody AgentRequest request, HttpServletRequest req, HttpServletResponse res) {
        return runtime.stream(request, context(req,res)).map(this::sse);
    }
    @PostMapping("/{runId}/cancel")
    public Result<Void> cancel(@PathVariable String runId,HttpServletRequest req,HttpServletResponse res){runtime.cancel(runId,context(req,res));return Result.success();}
    @GetMapping("/{runId}")
    public Result<AgentRun> get(@PathVariable String runId,HttpServletRequest req,HttpServletResponse res){return Result.success(runtime.getRun(runId,context(req,res)));}
    @GetMapping("/{runId}/events")
    public Result<List<AgentEvent>> events(@PathVariable String runId,@RequestParam(defaultValue="0")long after,HttpServletRequest req,HttpServletResponse res){return Result.success(runtime.events(runId,after,context(req,res)));}

    private AgentExecutionContext context(HttpServletRequest req,HttpServletResponse res){var p=principalResolver.resolve(req,res);var auth=SecurityContextHolder.getContext().getAuthentication();Set<String> permissions=auth==null?Set.of():auth.getAuthorities().stream().map(a->a.getAuthority()).collect(java.util.stream.Collectors.toUnmodifiableSet());return new AgentExecutionContext(p.userId(),p.guestTokenHash(),null,GatewayUserContext.getUsername(),p,permissions);}
    private ServerSentEvent<AgentEvent> sse(AgentEvent e){return ServerSentEvent.<AgentEvent>builder(e).id(e.getEventId()).event(e.getType().wireName()).build();}
}
