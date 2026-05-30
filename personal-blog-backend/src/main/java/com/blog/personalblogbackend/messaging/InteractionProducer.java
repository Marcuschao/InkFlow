package com.blog.personalblogbackend.messaging;

import com.blog.personalblogbackend.config.interaction.InteractionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InteractionProducer {
    private static final Logger log = LoggerFactory.getLogger(InteractionProducer.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final InteractionProperties properties;
    private final InteractionPersistHandler persistHandler;

    public InteractionProducer(ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
                               InteractionProperties properties,
                               InteractionPersistHandler persistHandler) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.properties = properties;
        this.persistHandler = persistHandler;
    }

    public void publish(InteractionPersistMessage message) {
        if (message.getMessageId() == null) {
            message.setMessageId(UUID.randomUUID().toString());
        }
        RabbitTemplate rabbit = rabbitTemplateProvider.getIfAvailable();
        if (rabbit != null) {
            try {
                CorrelationData correlation = new CorrelationData(message.getMessageId());
                rabbit.convertAndSend(properties.getExchange(), properties.getRoutingKey(), message, correlation);
                return;
            } catch (Exception ex) {
                log.warn("interaction mq publish failed, fallback sync messageId={}", message.getMessageId(), ex);
            }
        }
        if (properties.isSyncFallback()) {
            persistHandler.handle(message);
        }
    }
}
