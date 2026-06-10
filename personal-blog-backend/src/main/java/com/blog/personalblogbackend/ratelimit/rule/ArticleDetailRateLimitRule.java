package com.blog.personalblogbackend.ratelimit.rule;

import com.blog.personalblogbackend.config.ratelimit.RateLimitProperties;
import com.blog.personalblogbackend.ratelimit.RateLimitRuleResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Order(30)
@Component
public class ArticleDetailRateLimitRule implements RateLimitRule {

    private static final Pattern ARTICLE_DETAIL = Pattern.compile("^/api/articles/\\d+$");

    private final RateLimitProperties properties;

    public ArticleDetailRateLimitRule(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && ARTICLE_DETAIL.matcher(request.getRequestURI()).matches();
    }

    @Override
    public RateLimitRuleResolver.ResolvedRule resolve() {
        return new RateLimitRuleResolver.ResolvedRule("article:detail", properties.getArticleDetailPerMinute());
    }
}
