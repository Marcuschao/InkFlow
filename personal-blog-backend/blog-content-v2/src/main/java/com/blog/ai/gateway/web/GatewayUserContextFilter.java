package com.blog.ai.gateway.web;

import com.blog.ai.gateway.context.GatewayUserContext;
import com.blog.common.security.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GatewayUserContextFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public GatewayUserContextFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String bearer = request.getHeader("Authorization");
            if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
                try {
                    Claims claims = jwtUtils.parseToken(bearer.substring(7));
                    Long userId = claims.get("userId", Long.class);
                    String username = claims.get("username", String.class);
                    GatewayUserContext.set(userId, username);
                } catch (Exception ignored) {
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            GatewayUserContext.clear();
        }
    }
}
