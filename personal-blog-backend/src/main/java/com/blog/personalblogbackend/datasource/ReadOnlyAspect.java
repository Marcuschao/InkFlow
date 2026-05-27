package com.blog.personalblogbackend.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ReadOnlyAspect {
    @Around("@annotation(readOnly)")
    public Object around(ProceedingJoinPoint pjp, ReadOnly readOnly) throws Throwable {
        ReadWriteContext.markReadOnly();
        try {
            return pjp.proceed();
        } finally {
            ReadWriteContext.clear();
        }
    }
}
