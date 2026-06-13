package com.blog.auth.config.audit;

import com.blog.auth.common.support.Result;
import com.blog.auth.common.support.web.ClientIp;
import com.blog.auth.model.dto.LoginRequest;
import com.blog.auth.service.AuditLogQueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditLogQueryService auditLogQueryService;

    @Around("@annotation(audit)")
    public Object aroundAudit(ProceedingJoinPoint pjp, Audit audit) throws Throwable {
        Object out = pjp.proceed();
        try {
            if (out instanceof Result<?> r && r.getCode() != null && r.getCode() == 200) {
                String user = resolveUser(pjp);
                String detail = buildDetail(pjp, audit.value());
                String ip = resolveIp();
                auditLogQueryService.record(user, audit.value(), detail, ip);
            }
        } catch (RuntimeException ignored) {
            /* skip audit persistence failures */
        }
        return out;
    }

    private static String resolveUser(ProceedingJoinPoint pjp) {
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof LoginRequest lr && lr.getUsername() != null) {
                return lr.getUsername();
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return auth.getName();
        }
        return "anonymous";
    }

    private static String resolveIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "";
        }
        HttpServletRequest req = attrs.getRequest();
        return ClientIp.resolve(req);
    }

    private static String buildDetail(ProceedingJoinPoint pjp, String action) {
        if ("DELETE_ARTICLE".equals(action)) {
            for (Object arg : pjp.getArgs()) {
                if (arg instanceof Long id) {
                    return "articleId=" + id;
                }
            }
        }
        return action;
    }
}
