package com.blog.personalblogbackend.monitor;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {
    private final RedisConnectionFactory connectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try (var conn = connectionFactory.getConnection()) {
            String pong = conn.ping();
            return Health.up().withDetail("ping", pong).build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
