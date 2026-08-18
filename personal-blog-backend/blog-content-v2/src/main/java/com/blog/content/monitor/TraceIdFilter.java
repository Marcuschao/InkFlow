package com.blog.content.monitor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = TraceIdSupport.resolveOrCreate(request.getHeader(TraceIdSupport.HEADER));
        TraceIdSupport.clearMdc();
        try {
            org.slf4j.MDC.put(TraceIdSupport.MDC_KEY, traceId);
            response.setHeader(TraceIdSupport.HEADER, traceId);
            chain.doFilter(request, response);
        } finally {
            TraceIdSupport.clearMdc();
        }
    }
}
