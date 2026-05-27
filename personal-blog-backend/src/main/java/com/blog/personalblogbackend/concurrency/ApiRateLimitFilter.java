package com.blog.personalblogbackend.concurrency;

import com.blog.personalblogbackend.common.support.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ApiRateLimitFilter(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api")) {
            chain.doFilter(request, response);
            return;
        }
        int limit = resolveLimit(request);
        String key = "rate:" + clientIp(request) + ":" + request.getMethod() + ":" + request.getRequestURI();
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofSeconds(60));
        }
        if (count != null && count > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail(429, "请求过于频繁，请稍后再试")));
            return;
        }
        chain.doFilter(request, response);
    }

    private static int resolveLimit(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if (uri.startsWith("/api/admin")) {
            return 120;
        }
        if ("/api/stat/view".equals(uri)) {
            return 300;
        }
        if ("GET".equalsIgnoreCase(method)) {
            return 300;
        }
        return 30;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
