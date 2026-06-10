package com.blog.personalblogbackend.ratelimit.rule;

import com.blog.personalblogbackend.ratelimit.RateLimitRuleResolver;
import jakarta.servlet.http.HttpServletRequest;

// 设计模式：责任链 - 限流规则节点，按顺序匹配请求并返回配额
public interface RateLimitRule {

    boolean matches(HttpServletRequest request);

    RateLimitRuleResolver.ResolvedRule resolve();
}
