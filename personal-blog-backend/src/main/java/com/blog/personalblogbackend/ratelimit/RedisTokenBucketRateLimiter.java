package com.blog.personalblogbackend.ratelimit;

import com.blog.personalblogbackend.config.ratelimit.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Lazy
public class RedisTokenBucketRateLimiter {
    private final RedissonClient redisson;
    private final RateLimitProperties properties;
    private final RateLimitRuleResolver ruleResolver;
    private final RateLimitMetricsRecorder metrics;
    private final Map<String, Integer> configuredRates = new ConcurrentHashMap<>();

    public RedisTokenBucketRateLimiter(RedissonClient redisson,
                                       RateLimitProperties properties,
                                       RateLimitRuleResolver ruleResolver,
                                       RateLimitMetricsRecorder metrics) {
        this.redisson = redisson;
        this.properties = properties;
        this.ruleResolver = ruleResolver;
        this.metrics = metrics;
    }

    public boolean tryAcquire(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        RateLimitRuleResolver.ResolvedRule rule = ruleResolver.resolve(request);
        String ip = RateLimitRuleResolver.clientIp(request);
        String limiterName = "rl:" + rule.routeKey() + ":" + ip;
        RRateLimiter limiter = redisson.getRateLimiter(limiterName);
        ensureRate(limiter, limiterName, rule.permitsPerMinute());
        if (limiter.tryAcquire(1)) {
            metrics.recordAllowed();
            return true;
        }
        metrics.recordRejected();
        return false;
    }

    private void ensureRate(RRateLimiter limiter, String name, int permitsPerMinute) {
        Integer cached = configuredRates.get(name);
        if (cached != null && cached == permitsPerMinute) {
            return;
        }
        synchronized (configuredRates) {
            cached = configuredRates.get(name);
            if (cached != null && cached == permitsPerMinute) {
                return;
            }
            if (!limiter.trySetRate(RateType.OVERALL, permitsPerMinute, 1, RateIntervalUnit.MINUTES)) {
                limiter.setRate(RateType.OVERALL, permitsPerMinute, 1, RateIntervalUnit.MINUTES);
            }
            configuredRates.put(name, permitsPerMinute);
        }
    }
}
