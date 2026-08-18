package com.blog.ai.runtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.runtime.config.AgentRuntimeProperties;
import com.blog.ai.runtime.core.AgentCancellationRegistry;
import com.blog.ai.runtime.core.AgentStateMachine;
import com.blog.ai.runtime.event.AgentEventStore;
import com.blog.ai.runtime.handler.AgentTaskChunk;
import com.blog.ai.runtime.handler.AgentTaskHandler;
import com.blog.ai.runtime.mapper.AgentRunMapper;
import com.blog.ai.runtime.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DefaultAgentRuntime implements AgentRuntime {
    private final Map<AgentTaskType, AgentTaskHandler> handlers = new ConcurrentHashMap<>();
    private final AgentRunMapper runMapper; private final AgentStateMachine stateMachine;
    private final AgentEventStore eventStore; private final AgentCancellationRegistry cancellations;
    private final AgentRuntimeProperties properties; private final ObjectMapper json; private final MeterRegistry metrics;
    private final AsyncTaskExecutor runtimeExecutor;

    public DefaultAgentRuntime(List<AgentTaskHandler> handlers, AgentRunMapper runMapper,
            AgentStateMachine stateMachine, AgentEventStore eventStore, AgentCancellationRegistry cancellations,
            AgentRuntimeProperties properties, ObjectMapper json, MeterRegistry metrics,
            @Qualifier("agentRuntimeExecutor") AsyncTaskExecutor runtimeExecutor) {
        for (AgentTaskHandler handler : handlers) {
            if (this.handlers.putIfAbsent(handler.supports(), handler) != null)
                throw new IllegalStateException("Duplicate task handler: " + handler.supports());
        }
        this.runMapper=runMapper; this.stateMachine=stateMachine; this.eventStore=eventStore;
        this.cancellations=cancellations; this.properties=properties; this.json=json; this.metrics=metrics; this.runtimeExecutor=runtimeExecutor;
    }

    @Override
    public AgentResult run(AgentRequest request, AgentExecutionContext context) {
        validate(request); AgentRun run=createRun(request,context); AtomicLong sequence=new AtomicLong();
        long started=System.nanoTime();
        try {
            emit(run,sequence,AgentEventType.RUN_STARTED,Map.of("taskType",request.getTaskType().name()));
            transition(run,AgentRunStatus.CLASSIFY,sequence); emit(run,sequence,AgentEventType.RUN_CLASSIFIED,Map.of("taskType",request.getTaskType().name()));
            if (request.getTaskType()==AgentTaskType.RAG_QA) {
                transition(run,AgentRunStatus.RETRIEVE,sequence); emit(run,sequence,AgentEventType.RETRIEVAL_STARTED,Map.of());
                transition(run,AgentRunStatus.PLAN,sequence);
            } else transition(run,AgentRunStatus.PLAN,sequence);
            checkBoundaries(run,request);
            if(request.getTaskType()==AgentTaskType.WORKFLOW)transition(run,AgentRunStatus.TOOL_CALL,sequence);
            transition(run,AgentRunStatus.GENERATE,sequence);
            AgentExecutionContext executionContext=context.withRun(run.getRunId(),run.getTraceId());
            if(request.getTaskType()==AgentTaskType.WORKFLOW)emit(run,sequence,AgentEventType.TOOL_STARTED,Map.of("tool",String.valueOf(request.getInput().get("name"))));
            AgentResult result=requireHandler(request.getTaskType()).execute(request,executionContext);
            if(request.getTaskType()==AgentTaskType.WORKFLOW)emit(run,sequence,AgentEventType.TOOL_COMPLETED,Map.of("success",true));
            enforceTokenBudget(request, result.getInputTokens() + result.getOutputTokens());
            transition(run,AgentRunStatus.SAFETY_CHECK,sequence);
            result.setRunId(run.getRunId()); result.setTraceId(run.getTraceId()); result.setStatus(AgentRunStatus.COMPLETED);
            finish(run,result,AgentRunStatus.COMPLETED,null,null);
            emit(run,sequence,AgentEventType.RUN_COMPLETED,resultData(result));
            recordDuration(request.getTaskType(),"completed",started); return result;
        } catch(Exception ex) {
            AgentResult failure=fail(run,sequence,request,ex,started); throw ex instanceof ServiceException se?se:new ServiceException(500,failure.getErrorMessage());
        } finally { cancellations.clear(run.getRunId()); }
    }

    @Override
    public Flux<AgentEvent> stream(AgentRequest request, AgentExecutionContext context) {
        validate(request);
        return Flux.defer(() -> {
            AgentRun run=createRun(request,context); AtomicLong sequence=new AtomicLong(); long started=System.nanoTime();
            List<AgentEvent> initial=new ArrayList<>();
            initial.add(emit(run,sequence,AgentEventType.RUN_STARTED,Map.of("taskType",request.getTaskType().name())));
            transition(run,AgentRunStatus.CLASSIFY,sequence); initial.add(emit(run,sequence,AgentEventType.RUN_CLASSIFIED,Map.of("taskType",request.getTaskType().name())));
            if(request.getTaskType()==AgentTaskType.RAG_QA){transition(run,AgentRunStatus.RETRIEVE,sequence);initial.add(emit(run,sequence,AgentEventType.RETRIEVAL_STARTED,Map.of()));transition(run,AgentRunStatus.PLAN,sequence);}else transition(run,AgentRunStatus.PLAN,sequence);
            if(request.getTaskType()==AgentTaskType.WORKFLOW)transition(run,AgentRunStatus.TOOL_CALL,sequence);
            transition(run,AgentRunStatus.GENERATE,sequence);
            AtomicLong streamedCharacters = new AtomicLong();
            AgentExecutionContext executionContext=context.withRun(run.getRunId(),run.getTraceId());
            Flux<AgentEvent> execution=requireHandler(request.getTaskType()).stream(request,executionContext)
                    .timeout(resolveTimeout(request))
                    .takeUntil(ignored->cancellations.isCancelled(run.getRunId()))
                    .concatMap(chunk->{
                        if(chunk.type()==AgentEventType.DELTA && chunk.data().get("delta") instanceof String delta){
                            long estimatedTokens=streamedCharacters.addAndGet(delta.length())/4;
                            enforceTokenBudget(request, estimatedTokens);
                        }
                        return toEvents(run,sequence,chunk);
                    })
                    .onErrorResume(ex->Flux.fromIterable(failureEvents(run,sequence,request,ex,started)))
                    .doOnCancel(()->cancelInternal(run,sequence,context))
                    .doFinally(signal->{cancellations.clear(run.getRunId());recordDuration(request.getTaskType(),run.getStatus().toLowerCase(),started);});
            return Flux.concat(Flux.fromIterable(initial),execution)
                    .subscribeOn(Schedulers.fromExecutor(runtimeExecutor));
        });
    }

    private Flux<AgentEvent> toEvents(AgentRun run,AtomicLong sequence,AgentTaskChunk chunk){
        if(chunk.result()==null)return Flux.just(emit(run,sequence,chunk.type(),chunk.data()));
        AgentResult result=chunk.result(); result.setRunId(run.getRunId());result.setTraceId(run.getTraceId());
        transition(run,AgentRunStatus.SAFETY_CHECK,sequence); result.setStatus(AgentRunStatus.COMPLETED);
        finish(run,result,AgentRunStatus.COMPLETED,null,null);
        return Flux.just(emit(run,sequence,AgentEventType.VALIDATION_COMPLETED,Map.of("grounded",result.isGrounded(),"confidence",result.getConfidence())),
                emit(run,sequence,AgentEventType.RUN_COMPLETED,resultData(result)));
    }

    @Override public void cancel(String runId,AgentExecutionContext context){AgentRun run=requireOwnedRun(runId,context);if(runStatus(run).terminal())return;cancellations.cancel(runId);finish(run,null,AgentRunStatus.CANCELLED,AgentErrorCode.AGENT_CANCELLED.name(),"Cancelled by user");}
    @Override public AgentRun getRun(String runId,AgentExecutionContext context){return requireOwnedRun(runId,context);}
    @Override public List<AgentEvent> events(String runId,long afterSequence,AgentExecutionContext context){requireOwnedRun(runId,context);return eventStore.replay(runId,afterSequence,properties.getEventReplayLimit());}

    private AgentRun createRun(AgentRequest request,AgentExecutionContext context){
        LocalDateTime now=LocalDateTime.now(); AgentRun run=new AgentRun(); run.setRunId(UUID.randomUUID().toString());run.setTraceId(UUID.randomUUID().toString().replace("-",""));
        run.setSessionId(request.getSessionId());run.setUserId(context.userId());run.setGuestIdHash(context.guestIdHash());run.setTenantId(context.tenantId());run.setTaskType(request.getTaskType().name());
        run.setAgentName("inkflow-agent-runtime");run.setAgentVersion("1.0.0");run.setPromptVersion("runtime-v1");run.setStatus(AgentRunStatus.START.name());run.setStepCount(0);
        run.setInputTokens(0);run.setOutputTokens(0);run.setCost(0D);run.setVersion(0);run.setStartedAt(now);run.setCreatedAt(now);run.setUpdatedAt(now);runMapper.insert(run);return run;
    }
    private void transition(AgentRun run,AgentRunStatus next,AtomicLong sequence){AgentRunStatus current=runStatus(run);stateMachine.assertTransition(current,next);int changed=runMapper.transition(run.getRunId(),current.name(),next.name(),run.getVersion());if(changed!=1)throw new IllegalStateException("Concurrent agent state update: "+run.getRunId());run.setStatus(next.name());run.setVersion(run.getVersion()+1);run.setStepCount(run.getStepCount()+1);emit(run,sequence,AgentEventType.STATE_CHANGED,Map.of("from",current.name(),"to",next.name()));}
    private AgentEvent emit(AgentRun run,AtomicLong sequence,AgentEventType type,Map<String,Object> data){long seq=sequence.incrementAndGet();AgentEvent event=AgentEvent.builder().eventId(seq+"-0").sequence(seq).runId(run.getRunId()).traceId(run.getTraceId()).type(type).data(data).timestamp(LocalDateTime.now()).build();eventStore.append(event);metrics.counter("agent.runtime.events","type",type.wireName()).increment();return event;}
    private void finish(AgentRun run,AgentResult result,AgentRunStatus status,String code,String message){String payload=result==null?null:write(result);int changed=runMapper.finish(run.getRunId(),status.name(),payload,result==null?null:result.getModel(),result==null?0:result.getInputTokens(),result==null?0:result.getOutputTokens(),result==null?0D:result.getCost(),code,message);if(changed>0){run.setStatus(status.name());run.setErrorCode(code);run.setErrorMessage(message);run.setFinishedAt(LocalDateTime.now());}}
    private AgentResult fail(AgentRun run,AtomicLong sequence,AgentRequest request,Exception ex,long started){AgentRunStatus status=failureStatus(ex);String code=switch(status){case TIMEOUT->AgentErrorCode.AGENT_TIMEOUT.name();case BUDGET_EXCEEDED->AgentErrorCode.AGENT_BUDGET_EXCEEDED.name();case CANCELLED->AgentErrorCode.AGENT_CANCELLED.name();default->AgentErrorCode.AGENT_MODEL_UNAVAILABLE.name();};AgentResult r=new AgentResult();r.setRunId(run.getRunId());r.setTraceId(run.getTraceId());r.setStatus(status);r.setErrorCode(code);r.setErrorMessage(ex.getMessage());finish(run,r,status,code,ex.getMessage());emit(run,sequence,AgentEventType.RUN_FAILED,errorData(code,ex.getMessage()));recordDuration(request.getTaskType(),status.name().toLowerCase(),started);return r;}
    private List<AgentEvent> failureEvents(AgentRun run,AtomicLong sequence,AgentRequest request,Throwable ex,long started){if(cancellations.isCancelled(run.getRunId())){finish(run,null,AgentRunStatus.CANCELLED,AgentErrorCode.AGENT_CANCELLED.name(),"Cancelled");return List.of(emit(run,sequence,AgentEventType.RUN_CANCELLED,Map.of("errorCode",AgentErrorCode.AGENT_CANCELLED.name())));}Exception wrapped=ex instanceof Exception e?e:new RuntimeException(ex);AgentResult result=fail(run,sequence,request,wrapped,started);return List.of(emit(run,sequence,AgentEventType.ERROR,errorData(result.getErrorCode(),ex.getMessage())));}
    private void cancelInternal(AgentRun run,AtomicLong sequence,AgentExecutionContext context){if(!runStatus(run).terminal()){cancellations.cancel(run.getRunId());finish(run,null,AgentRunStatus.CANCELLED,AgentErrorCode.AGENT_CANCELLED.name(),"Client disconnected");emit(run,sequence,AgentEventType.RUN_CANCELLED,Map.of("errorCode",AgentErrorCode.AGENT_CANCELLED.name()));}}
    private AgentRun requireOwnedRun(String runId,AgentExecutionContext context){AgentRun run=runMapper.selectOne(new LambdaQueryWrapper<AgentRun>().eq(AgentRun::getRunId,runId));if(run==null)throw new ServiceException(404,"Agent run not found");if(!context.owns(run))throw new ServiceException(403,"Agent run access denied");return run;}
    private AgentTaskHandler requireHandler(AgentTaskType type){AgentTaskHandler handler=handlers.get(type);if(handler==null)throw new ServiceException(400,AgentErrorCode.AGENT_UNSUPPORTED_TASK.name());return handler;}
    private void validate(AgentRequest request){if(request==null||request.getTaskType()==null)throw new ServiceException(400,AgentErrorCode.AGENT_INVALID_REQUEST.name());}
    private void checkBoundaries(AgentRun run,AgentRequest request){int max=request.getMaxSteps()==null?properties.getMaxSteps():request.getMaxSteps();if(run.getStepCount()>=max)throw new ServiceException(429,AgentErrorCode.AGENT_BUDGET_EXCEEDED.name());if(cancellations.isCancelled(run.getRunId()))throw new ServiceException(409,AgentErrorCode.AGENT_CANCELLED.name());}
    private void enforceTokenBudget(AgentRequest request,long used){int budget=request.getTokenBudget()==null?properties.getTokenBudget():request.getTokenBudget();if(used>budget)throw new ServiceException(429,AgentErrorCode.AGENT_BUDGET_EXCEEDED.name()+": token budget exceeded");}
    private AgentRunStatus failureStatus(Exception ex){String message=ex.getMessage()==null?"":ex.getMessage();if(ex instanceof java.util.concurrent.TimeoutException||message.contains("Timeout"))return AgentRunStatus.TIMEOUT;if(message.contains(AgentErrorCode.AGENT_BUDGET_EXCEEDED.name()))return AgentRunStatus.BUDGET_EXCEEDED;if(message.contains(AgentErrorCode.AGENT_CANCELLED.name()))return AgentRunStatus.CANCELLED;return AgentRunStatus.FAILED;}
    private Duration resolveTimeout(AgentRequest r){return r.getTimeoutMs()==null?properties.getTotalTimeout():Duration.ofMillis(Math.min(r.getTimeoutMs(),properties.getTotalTimeout().toMillis()));}
    private AgentRunStatus runStatus(AgentRun run){return AgentRunStatus.valueOf(run.getStatus());}
    private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception ex){throw new IllegalStateException(ex);}}
    private Map<String,Object> resultData(AgentResult r){Map<String,Object>d=new LinkedHashMap<>();d.put("result",r);d.put("sessionId",r.getSessionId());d.put("messageId",r.getMessageId());return d;}
    private Map<String,Object> errorData(String code,String message){Map<String,Object>d=new LinkedHashMap<>();d.put("errorCode",code);d.put("message",message==null?"Agent execution failed":message);return d;}
    private void recordDuration(AgentTaskType task,String status,long started){metrics.timer("agent.runtime.duration","task",task.name(),"status",status).record(System.nanoTime()-started,java.util.concurrent.TimeUnit.NANOSECONDS);metrics.counter("agent.runtime.runs","task",task.name(),"status",status).increment();}
}
