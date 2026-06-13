package com.blog.content.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ContentChangeProducer {
    private static final Logger log = LoggerFactory.getLogger(ContentChangeProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public ContentChangeProducer(RabbitTemplate rabbitTemplate,
                                 @Value("${blog.content.exchange:blog.content}") String exchange,
                                 @Value("${blog.content.routing-key:content.change}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void send(Long articleId, ContentChangeEventType eventType) {
        if (articleId == null || eventType == null) {
            return;
        }
        ContentChangeMessage message = new ContentChangeMessage(UUID.randomUUID().toString(), articleId, eventType);
        CorrelationData correlationData = new CorrelationData(message.getMessageId());
        rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
    }
}
