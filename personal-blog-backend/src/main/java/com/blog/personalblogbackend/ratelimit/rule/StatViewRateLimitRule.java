package com.blog.personalblogbackend.ratelimit.rule;

import com.blog.personalblogbackend.config.ratelimit.RateLimitProperties;
import com.blog.personalblogbackend.ratelimit.RateLimitRuleResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(20)
@Component
public class StatViewRateLimitRule implements RateLimitRule {

    private final RateLimitProperties properties;

    public StatViewRateLimitRule(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return "/api/stat/view".equals(request.getRequestURI());
    }

    @Override
    public RateLimitRuleResolver.ResolvedRule resolve() {
        return new RateLimitRuleResolver.ResolvedRule("stat:view", properties.getStatViewPerMinute());
    }
}
