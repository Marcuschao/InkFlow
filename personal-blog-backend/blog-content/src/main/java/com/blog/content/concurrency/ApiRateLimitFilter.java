package com.blog.content.concurrency;

import com.blog.content.common.support.Result;
import com.blog.content.ratelimit.RedisTokenBucketRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final RedisTokenBucketRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public ApiRateLimitFilter(RedisTokenBucketRateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api")) {
            chain.doFilter(request, response);
            return;
        }
        if (!rateLimiter.tryAcquire(request)) {
            response.setStatus(429);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail(429, "请求过于频繁，请稍后再试")));
            return;
        }
        chain.doFilter(request, response);
    }
}
