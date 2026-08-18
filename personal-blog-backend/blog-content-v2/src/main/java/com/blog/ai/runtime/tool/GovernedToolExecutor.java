package com.blog.ai.runtime.tool;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.runtime.mapper.AgentToolCallMapper;
import com.blog.ai.runtime.model.AgentErrorCode;
import com.blog.ai.runtime.model.AgentExecutionContext;
import com.blog.ai.runtime.model.AgentToolCall;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
@Component
public class GovernedToolExecutor {
    private final ToolRegistry registry; private final ToolPolicyEngine policy; private final AgentToolCallMapper mapper;
    private final ObjectMapper json; private final Validator validator; private final AsyncTaskExecutor executor;
    private final ToolSchemaValidator schemaValidator;
    public GovernedToolExecutor(ToolRegistry registry, ToolPolicyEngine policy, AgentToolCallMapper mapper,
            ObjectMapper json, Validator validator, ToolSchemaValidator schemaValidator,
            @Qualifier("agentToolExecutor") AsyncTaskExecutor executor) {
        this.registry=registry; this.policy=policy; this.mapper=mapper; this.json=json; this.validator=validator; this.schemaValidator=schemaValidator; this.executor=executor;
    }
    public ToolResult execute(ToolInvocation i, AgentExecutionContext c) {
        ToolHandler<?> raw=registry.require(i.getName(),i.getVersion()); ToolDefinition d=raw.definition(); policy.authorize(d,i,c);
        if (StringUtils.hasText(i.getIdempotencyKey())) {
            Long count=mapper.selectCount(new LambdaQueryWrapper<AgentToolCall>().eq(AgentToolCall::getIdempotencyKey,i.getIdempotencyKey()));
            if (count!=null && count>0) throw new ServiceException(409,AgentErrorCode.AGENT_TOOL_DENIED.name()+": duplicate idempotency key");
        }
        schemaValidator.validate(d.parameterSchema(),i.getArguments());
        Object args=json.convertValue(i.getArguments(),raw.argumentType()); Set<ConstraintViolation<Object>> violations=validator.validate(args);
        if (!violations.isEmpty()) { ConstraintViolation<Object> v=violations.iterator().next(); throw new ServiceException(400,AgentErrorCode.AGENT_TOOL_INVALID_ARGUMENT.name()+": "+v.getPropertyPath()+" "+v.getMessage()); }
        AgentToolCall audit=begin(i,d,c,args); long started=System.nanoTime();
        try { ToolResult result=invokeWithRetry(raw,args,c,d); finish(audit,result.isSuccess()?"SUCCESS":"FAILED",digest(result.getData()),started); return result; }
        catch(Exception ex) { finish(audit,"FAILED",digest(ex.getMessage()),started); throw new ServiceException(502,"Agent tool execution failed: "+ex.getMessage()); }
    }
    @SuppressWarnings("unchecked") private <T> ToolResult invoke(ToolHandler<?> h,Object a,AgentExecutionContext c){ return ((ToolHandler<T>)h).execute((T)a,c); }
    private ToolResult invokeWithRetry(ToolHandler<?> handler,Object args,AgentExecutionContext context,ToolDefinition definition)throws Exception{
        Exception last=null;
        int retries = definition.riskLevel() == ToolRiskLevel.READ_ONLY || definition.idempotencyRequired()
                ? definition.maxRetries() : 0;
        for(int attempt=0;attempt<=retries;attempt++){
            try{Future<ToolResult> future=executor.submit(()->invoke(handler,args,context));return future.get(definition.timeout().toMillis(),TimeUnit.MILLISECONDS);}
            catch(Exception ex){last=ex;if(attempt>=retries)break;long backoff=Math.min(2000L,100L*(1L<<Math.min(attempt,4)))+java.util.concurrent.ThreadLocalRandom.current().nextLong(50L);Thread.sleep(backoff);}
        }
        throw last==null?new IllegalStateException("Tool execution failed"):last;
    }
    private AgentToolCall begin(ToolInvocation i,ToolDefinition d,AgentExecutionContext c,Object args){
        AgentToolCall r=new AgentToolCall(); r.setToolCallId(UUID.randomUUID().toString()); r.setRunId(i.getRunId()); r.setTraceId(i.getTraceId());
        r.setToolName(d.name()); r.setToolVersion(d.version()); r.setRiskLevel(d.riskLevel().name()); r.setIdempotencyKey(i.getIdempotencyKey());
        r.setArgumentsDigest(digest(args)); r.setValidationStatus("VALID"); r.setApprovalStatus(d.adminApprovalRequired()?"ADMIN_APPROVED":d.userConfirmationRequired()?"USER_CONFIRMED":"NOT_REQUIRED");
        r.setExecutionStatus("RUNNING"); r.setOperatorUserId(c.userId()); r.setCreatedAt(LocalDateTime.now()); mapper.insert(r); return r;
    }
    private void finish(AgentToolCall r,String status,String digest,long started){ r.setExecutionStatus(status); r.setResultDigest(digest); r.setDurationMs(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started)); r.setFinishedAt(LocalDateTime.now()); mapper.updateById(r); }
    private String digest(Object v){ try{return DigestUtils.md5DigestAsHex(json.writeValueAsBytes(v));}catch(Exception ex){return DigestUtils.md5DigestAsHex(String.valueOf(v).getBytes(java.nio.charset.StandardCharsets.UTF_8));} }
}
