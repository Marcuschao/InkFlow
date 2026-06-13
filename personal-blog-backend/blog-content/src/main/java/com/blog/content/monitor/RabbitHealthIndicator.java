package com.blog.content.monitor;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class RabbitHealthIndicator implements HealthIndicator {
    private final ConnectionFactory connectionFactory;

    public RabbitHealthIndicator(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try (var conn = connectionFactory.createConnection()) {
            return conn.isOpen() ? Health.up().build() : Health.down().withDetail("reason", "closed").build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
