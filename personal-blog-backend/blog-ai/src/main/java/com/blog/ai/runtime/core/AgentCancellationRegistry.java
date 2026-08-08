package com.blog.ai.runtime.core;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentCancellationRegistry {
    private static final String PREFIX = "ai:agent:cancel:";
    private final Set<String> local = ConcurrentHashMap.newKeySet();
    private final StringRedisTemplate redisTemplate;

    public AgentCancellationRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cancel(String runId) {
        local.add(runId);
        try { redisTemplate.opsForValue().set(PREFIX + runId, "1", Duration.ofHours(2)); }
        catch (RuntimeException ignored) { }
    }

    public boolean isCancelled(String runId) {
        if (local.contains(runId)) return true;
        try { return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + runId)); }
        catch (RuntimeException ignored) { return false; }
    }

    public void clear(String runId) {
        local.remove(runId);
        try { redisTemplate.delete(PREFIX + runId); }
        catch (RuntimeException ignored) { }
    }
}
