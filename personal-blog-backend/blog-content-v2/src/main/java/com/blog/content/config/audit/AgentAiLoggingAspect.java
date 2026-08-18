package com.blog.content.config.audit;

import com.blog.content.service.AiCallLogWriter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AgentAiLoggingAspect {

    @Autowired
    private AiCallLogWriter aiCallLogWriter;

    @Around("execution(* com.blog.content.service.impl.AgentServiceImpl.*(..))")
    public Object aroundAgent(ProceedingJoinPoint pjp) throws Throwable {
        String feature = pjp.getSignature().getName();
        long t0 = System.currentTimeMillis();
        boolean ok = true;
        try {
            //调用原本的业务逻辑
            return pjp.proceed();
        } catch (Throwable ex) {
            ok = false;
            throw ex;
        } finally {
            long ms = System.currentTimeMillis() - t0;
            aiCallLogWriter.write(feature, ok, ms);
        }
    }
}
