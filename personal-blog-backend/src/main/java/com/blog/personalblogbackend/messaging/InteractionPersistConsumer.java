package com.blog.personalblogbackend.messaging;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "blog.notification.enabled", havingValue = "true", matchIfMissing = true)
public class InteractionPersistConsumer {
    private static final Logger log = LoggerFactory.getLogger(InteractionPersistConsumer.class);

    private final InteractionPersistHandler persistHandler;
    private final MqIdempotencyService idempotencyService;

    public InteractionPersistConsumer(InteractionPersistHandler persistHandler,
                                      MqIdempotencyService idempotencyService) {
        this.persistHandler = persistHandler;
        this.idempotencyService = idempotencyService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${blog.interaction.queue:interaction.persist.queue}", durable = "true"),
            exchange = @Exchange(value = "${blog.interaction.exchange:blog.interaction}", type = ExchangeTypes.TOPIC, durable = "true"),
            key = "${blog.interaction.routing-key:interaction.persist}"
    ))
    public void onMessage(InteractionPersistMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        if (message == null || message.getMessageId() == null) {
            channel.basicAck(tag, false);
            return;
        }
        if (!idempotencyService.markIfAbsent("interaction", message.getMessageId())) {
            channel.basicAck(tag, false);
            return;
        }
        try {
            persistHandler.handle(message);
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.error("interaction persist failed messageId={}", message.getMessageId(), ex);
            channel.basicNack(tag, false, true);
        }
    }
}
