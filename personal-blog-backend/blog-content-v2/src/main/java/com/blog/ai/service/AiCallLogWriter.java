package com.blog.ai.service;

import com.blog.ai.mapper.AiCallLogMapper;
import com.blog.ai.model.entity.AiCallLog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AiCallLogWriter {

    @Autowired
    private AiCallLogMapper aiCallLogMapper;

    public void write(String feature, boolean success, long durationMs) {
        AiCallLog row = new AiCallLog();
        row.setFeature(feature);
        row.setSuccess(success ? 1 : 0);
        row.setDurationMs(durationMs);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            row.setUsername(auth.getName());
        }
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            String ip = req.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) {
                row.setClientIp(ip.split(",")[0].trim());
            } else {
                row.setClientIp(req.getRemoteAddr());
            }
        }
        aiCallLogMapper.insert(row);
    }
}
