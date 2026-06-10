package com.blog.personalblogbackend.ratelimit.rule;

import com.blog.personalblogbackend.config.ratelimit.RateLimitProperties;
import com.blog.personalblogbackend.ratelimit.RateLimitRuleResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(60)
@Component
public class ApiWriteRateLimitRule implements RateLimitRule {

    private final RateLimitProperties properties;

    public ApiWriteRateLimitRule(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return true;
    }

    @Override
    public RateLimitRuleResolver.ResolvedRule resolve() {
        return new RateLimitRuleResolver.ResolvedRule("api:write", properties.getApiWritePerMinute());
    }
}
