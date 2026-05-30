package com.blog.personalblogbackend.ratelimit;

import com.blog.personalblogbackend.config.ratelimit.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class RateLimitRuleResolver {
    private static final Pattern ARTICLE_DETAIL = Pattern.compile("^/api/articles/\\d+$");

    private final RateLimitProperties properties;

    public RateLimitRuleResolver(RateLimitProperties properties) {
        this.properties = properties;
    }

    public ResolvedRule resolve(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if (uri.startsWith("/api/admin")) {
            return new ResolvedRule("admin", properties.getAdminPerMinute());
        }
        if ("/api/stat/view".equals(uri)) {
            return new ResolvedRule("stat:view", properties.getStatViewPerMinute());
        }
        if ("GET".equalsIgnoreCase(method) && ARTICLE_DETAIL.matcher(uri).matches()) {
            return new ResolvedRule("article:detail", properties.getArticleDetailPerMinute());
        }
        if ("GET".equalsIgnoreCase(method) && "/api/articles".equals(uri)) {
            return new ResolvedRule("article:list", properties.getArticleListPerMinute());
        }
        if ("GET".equalsIgnoreCase(method)) {
            return new ResolvedRule("api:get", properties.getApiGetPerMinute());
        }
        return new ResolvedRule("api:write", properties.getApiWritePerMinute());
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record ResolvedRule(String routeKey, int permitsPerMinute) {
    }
}
