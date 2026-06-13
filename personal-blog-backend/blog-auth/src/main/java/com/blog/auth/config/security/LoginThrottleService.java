package com.blog.auth.config.security;

import com.blog.auth.common.exception.ServiceException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginThrottleService {

    private static final int MAX_PER_MINUTE_IP = 20;
    private static final int MAX_FAILURES_BEFORE_LOCK = 5;
    private static final int LOCK_MINUTES = 15;

    private final Cache<String, AtomicInteger> ipBurst = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2))
            .maximumSize(50_000)
            .build();

    private final Cache<String, Integer> failures = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(50_000)
            .build();

    private final Cache<String, Boolean> locks = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(LOCK_MINUTES))
            .maximumSize(50_000)
            .build();

    private static String minuteBucketKey(String ip) {
        long bucket = Instant.now().getEpochSecond() / 60;
        return ip + ":" + bucket;
    }

    private static String failureKey(String username, String ip) {
        String u = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return u + "|" + (ip == null ? "" : ip);
    }

    public void checkIpBurst(String ip) {
        if (ip == null || ip.isEmpty()) {
            return;
        }
        String key = minuteBucketKey(ip);
        int n = ipBurst.get(key, k -> new AtomicInteger(0)).incrementAndGet();
        if (n > MAX_PER_MINUTE_IP) {
            throw new ServiceException(429, "请求过于频繁，请稍后再试");
        }
    }

    public void ensureNotLocked(String username, String ip) {
        String fk = failureKey(username, ip);
        if (Boolean.TRUE.equals(locks.getIfPresent(fk))) {
            throw new ServiceException(429, "登录尝试次数过多，请15分钟后再试");
        }
    }

    public void clearFailures(String username, String ip) {
        failures.invalidate(failureKey(username, ip));
    }

    public int recordAuthFailure(String username, String ip) {
        String fk = failureKey(username, ip);
        int cnt = failures.asMap().compute(fk, (k, v) -> v == null ? 1 : v + 1);
        if (cnt >= MAX_FAILURES_BEFORE_LOCK) {
            locks.put(fk, Boolean.TRUE);
            failures.invalidate(fk);
            throw new ServiceException(429, "登录尝试次数过多，请15分钟后再试");
        }
        return MAX_FAILURES_BEFORE_LOCK - cnt;
    }
}
