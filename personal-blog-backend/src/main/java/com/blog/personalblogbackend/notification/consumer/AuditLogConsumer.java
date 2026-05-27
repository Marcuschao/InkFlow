package com.blog.personalblogbackend.notification.consumer;

import com.blog.personalblogbackend.notification.NotificationMessage;
import com.blog.personalblogbackend.service.AuditLogQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "blog.notification.enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogConsumer.class);

    private final AuditLogQueryService auditLogQueryService;
    private final ObjectMapper objectMapper;

    public AuditLogConsumer(AuditLogQueryService auditLogQueryService, ObjectMapper objectMapper) {
        this.auditLogQueryService = auditLogQueryService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "#{notificationRabbitProperties.auditQueue}")
    public void onMessage(NotificationMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            String action = "EVENT_" + (message.getType() != null ? message.getType() : "UNKNOWN");
            String detail = objectMapper.writeValueAsString(message.getPayload());
            auditLogQueryService.record("system", action, detail, null);
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.warn("[notification] audit consume failed: {}", ex.toString());
            channel.basicNack(tag, false, false);
        }
    }
}
