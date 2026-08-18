package com.blog.ai.runtime.handler;

import com.blog.ai.model.dto.agent.*;
import com.blog.ai.runtime.model.*;
import com.blog.ai.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.blog.ai.runtime.tool.GovernedToolExecutor;
import com.blog.ai.runtime.tool.ToolInvocation;
import com.blog.ai.runtime.tool.ToolResult;
import reactor.core.publisher.Flux;
import org.springframework.stereotype.Component;

import java.util.Map;

public abstract class BusinessTaskHandler implements AgentTaskHandler {
    protected final AgentService service; protected final ObjectMapper json;
    protected BusinessTaskHandler(AgentService service,ObjectMapper json){this.service=service;this.json=json;}
    protected <T> T body(AgentRequest r,Class<T> type){return json.convertValue(r.getInput(),type);}
    protected AgentResult text(Object value){AgentResult r=new AgentResult();r.setStatus(AgentRunStatus.COMPLETED);r.setAnswer(value==null?"":String.valueOf(value));return r;}
    protected AgentResult structured(Object value){AgentResult r=new AgentResult();r.setStatus(AgentRunStatus.COMPLETED);r.setOutput(Map.of("result",value));return r;}

    @Component
    public static class Writing extends BusinessTaskHandler {
        public Writing(AgentService s,ObjectMapper j){super(s,j);} public AgentTaskType supports(){return AgentTaskType.WRITING;}
        public AgentResult execute(AgentRequest r,AgentExecutionContext c){return switch(requireOperation(r)){
            case "outline"->text(service.outline(body(r,OutlineRequest.class))); case "expand"->text(service.expand(body(r,ExpandRequest.class)));
            case "polish"->text(service.polish(body(r,PolishRequest.class))); case "editor-outline"->text(service.editorOutline(body(r,EditorOutlineRequest.class)));
            case "editor-continue"->text(service.editorContinue(body(r,EditorContinueRequest.class))); case "editor-polish"->text(service.editorPolish(body(r,EditorPolishRequest.class)));
            case "summary"->text(service.summary(body(r,SummaryRequest.class))); case "tags"->structured(service.tags(body(r,TagsRequest.class)));
            default->throw new IllegalArgumentException("Unsupported WRITING operation: "+r.getOperation());};}
    }
    @Component
    public static class Recommendation extends BusinessTaskHandler {
        public Recommendation(AgentService s,ObjectMapper j){super(s,j);} public AgentTaskType supports(){return AgentTaskType.RECOMMENDATION;}
        public AgentResult execute(AgentRequest r,AgentExecutionContext c){return switch(requireOperation(r)){
            case "article"->structured(service.recommend(r.getArticleId()));
            case "context"->{RecommendContextRequest b=body(r,RecommendContextRequest.class);yield structured(service.recommendWithContext(b.getArticleId(),b.getRecentArticleIds()));}
            case "home"->{RecommendHomeRequest b=body(r,RecommendHomeRequest.class);yield structured(service.recommendHome(b.getRecentArticleIds()));}
            default->throw new IllegalArgumentException("Unsupported RECOMMENDATION operation: "+r.getOperation());};}
    }
    @Component
    public static class Report extends BusinessTaskHandler {
        public Report(AgentService s,ObjectMapper j){super(s,j);} public AgentTaskType supports(){return AgentTaskType.REPORT;}
        public AgentResult execute(AgentRequest r,AgentExecutionContext c){return text(service.weeklyReport(body(r,WeeklyReportRequest.class)));}
    }
    @Component
    public static class Workflow extends BusinessTaskHandler {
        private final GovernedToolExecutor tools;
        public Workflow(AgentService s,ObjectMapper j,GovernedToolExecutor tools){super(s,j);this.tools=tools;} public AgentTaskType supports(){return AgentTaskType.WORKFLOW;}
        public AgentResult execute(AgentRequest r,AgentExecutionContext c){if(!"tool".equals(requireOperation(r)))throw new IllegalArgumentException("Unsupported WORKFLOW operation: "+r.getOperation());ToolInvocation invocation=json.convertValue(r.getInput(),ToolInvocation.class);invocation.setRunId(c.runId());invocation.setTraceId(c.traceId());ToolResult result=tools.execute(invocation,c);return structured(result);}
        public Flux<AgentTaskChunk> stream(AgentRequest r,AgentExecutionContext c){return Flux.defer(()->{ToolInvocation invocation=json.convertValue(r.getInput(),ToolInvocation.class);AgentTaskChunk started=AgentTaskChunk.event(AgentEventType.TOOL_STARTED,Map.of("tool",invocation.getName(),"version",invocation.getVersion()));return Flux.concat(Flux.just(started),reactor.core.publisher.Mono.fromCallable(()->execute(r,c)).flatMapMany(result->Flux.just(AgentTaskChunk.event(AgentEventType.TOOL_COMPLETED,Map.of("tool",invocation.getName(),"success",true)),AgentTaskChunk.result(result))));});}
    }
    private static String requireOperation(AgentRequest r){if(r.getOperation()==null||r.getOperation().isBlank())throw new IllegalArgumentException("operation must not be blank");return r.getOperation();}
}
