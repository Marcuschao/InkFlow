package com.blog.content.messaging;

import com.blog.content.config.properties.NotificationRabbitProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MqIdempotencyService {
    private final StringRedisTemplate redis;
    private final Duration ttl;

    public MqIdempotencyService(StringRedisTemplate redis, NotificationRabbitProperties notificationProps) {
        this.redis = redis;
        int days = notificationProps.getIdempotencyTtlDays() > 0 ? notificationProps.getIdempotencyTtlDays() : 7;
        this.ttl = Duration.ofDays(days);
    }

    public boolean markIfAbsent(String queue, String messageId) {
        String key = "mq:consumed:" + queue + ":" + messageId;
        Boolean ok = redis.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }
}
