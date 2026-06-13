package com.blog.content.monitor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RabbitListenerTraceAspect {

    @Around("@annotation(org.springframework.amqp.rabbit.annotation.RabbitListener)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } finally {
            TraceIdSupport.clearMdc();
        }
    }
}
