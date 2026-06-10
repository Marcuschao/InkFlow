package com.blog.personalblogbackend.ratelimit.rule;

import com.blog.personalblogbackend.config.ratelimit.RateLimitProperties;
import com.blog.personalblogbackend.ratelimit.RateLimitRuleResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(40)
@Component
public class ArticleListRateLimitRule implements RateLimitRule {

    private final RateLimitProperties properties;

    public ArticleListRateLimitRule(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && "/api/articles".equals(request.getRequestURI());
    }

    @Override
    public RateLimitRuleResolver.ResolvedRule resolve() {
        return new RateLimitRuleResolver.ResolvedRule("article:list", properties.getArticleListPerMinute());
    }
}
