package com.blog.personalblogbackend.ratelimit.rule;

import com.blog.personalblogbackend.config.ratelimit.RateLimitProperties;
import com.blog.personalblogbackend.ratelimit.RateLimitRuleResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(10)
@Component
public class AdminRateLimitRule implements RateLimitRule {

    private final RateLimitProperties properties;

    public AdminRateLimitRule(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/admin");
    }

    @Override
    public RateLimitRuleResolver.ResolvedRule resolve() {
        return new RateLimitRuleResolver.ResolvedRule("admin", properties.getAdminPerMinute());
    }
}
