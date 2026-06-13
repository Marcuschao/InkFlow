package com.blog.content.ratelimit;

import com.blog.content.ratelimit.rule.RateLimitRule;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RateLimitRuleResolver {

    private final List<RateLimitRule> rules;

    // 设计模式：责任链 - 按顺序遍历限流规则，首个匹配即返回
    public RateLimitRuleResolver(List<RateLimitRule> rules) {
        this.rules = rules;
    }

    public ResolvedRule resolve(HttpServletRequest request) {
        for (RateLimitRule rule : rules) {
            if (rule.matches(request)) {
                return rule.resolve();
            }
        }
        throw new IllegalStateException("No rate limit rule matched");
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
