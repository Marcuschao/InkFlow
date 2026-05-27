package com.blog.personalblogbackend.messaging;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MqIdempotencyService {
    private final StringRedisTemplate redis;

    public MqIdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean markIfAbsent(String queue, String messageId) {
        String key = "mq:consumed:" + queue + ":" + messageId;
        Boolean ok = redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(7));
        return Boolean.TRUE.equals(ok);
    }
}
