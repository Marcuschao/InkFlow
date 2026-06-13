package com.blog.content.ratelimit.rule;

import com.blog.content.config.ratelimit.RateLimitProperties;
import com.blog.content.ratelimit.RateLimitRuleResolver;
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
