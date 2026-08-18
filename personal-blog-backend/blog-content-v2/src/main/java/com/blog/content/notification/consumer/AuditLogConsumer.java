package com.blog.content.notification.consumer;

import com.blog.content.notification.NotificationConsumeHelper;
import com.blog.content.notification.NotificationMessage;
import com.blog.content.service.AuditLogQueryService;
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
public class AuditLogConsumer extends AbstractNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogConsumer.class);

    private final AuditLogQueryService auditLogQueryService;
    private final ObjectMapper objectMapper;

    public AuditLogConsumer(AuditLogQueryService auditLogQueryService,
                            ObjectMapper objectMapper,
                            NotificationConsumeHelper consumeHelper) {
        super(consumeHelper);
        this.auditLogQueryService = auditLogQueryService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "#{notificationRabbitProperties.auditQueue}")
    public void onMessage(NotificationMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        consume(message, channel, tag);
    }

    @Override
    protected String queueKey() {
        return NotificationConsumeHelper.QUEUE_AUDIT;
    }

    @Override
    protected void doProcess(NotificationMessage message) throws Exception {
        String action = "EVENT_" + (message.getType() != null ? message.getType() : "UNKNOWN");
        String detail = objectMapper.writeValueAsString(message.getPayload());
        auditLogQueryService.record("system", action, detail, null);
    }

    @Override
    protected void logFailure(Exception ex, NotificationMessage message) {
        log.warn("[notification] audit consume failed eventId={}: {}",
                message.getEventId(), ex.toString());
    }
}
