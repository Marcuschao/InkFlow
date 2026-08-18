package com.blog.content.ratelimit.rule;

import com.blog.content.config.ratelimit.RateLimitProperties;
import com.blog.content.ratelimit.RateLimitRuleResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(50)
@Component
public class ApiGetRateLimitRule implements RateLimitRule {

    private final RateLimitProperties properties;

    public ApiGetRateLimitRule(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod());
    }

    @Override
    public RateLimitRuleResolver.ResolvedRule resolve() {
        return new RateLimitRuleResolver.ResolvedRule("api:get", properties.getApiGetPerMinute());
    }
}
