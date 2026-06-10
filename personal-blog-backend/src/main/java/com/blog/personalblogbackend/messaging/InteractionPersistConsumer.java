package com.blog.personalblogbackend.messaging;

import com.blog.personalblogbackend.messaging.support.AbstractIdempotentRabbitConsumer;
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
public class InteractionPersistConsumer extends AbstractIdempotentRabbitConsumer<InteractionPersistMessage> {
    private static final Logger log = LoggerFactory.getLogger(InteractionPersistConsumer.class);

    private final InteractionPersistHandler persistHandler;

    public InteractionPersistConsumer(InteractionPersistHandler persistHandler,
                                      MqIdempotencyService idempotencyService) {
        super(idempotencyService);
        this.persistHandler = persistHandler;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${blog.interaction.queue:interaction.persist.queue}", durable = "true"),
            exchange = @Exchange(value = "${blog.interaction.exchange:blog.interaction}", type = ExchangeTypes.TOPIC, durable = "true"),
            key = "${blog.interaction.routing-key:interaction.persist}"
    ))
    public void onMessage(InteractionPersistMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        consume(message, channel, tag);
    }

    @Override
    protected String idempotencyQueue() {
        return "interaction";
    }

    @Override
    protected String extractMessageId(InteractionPersistMessage message) {
        return message != null ? message.getMessageId() : null;
    }

    @Override
    protected void doProcess(InteractionPersistMessage message) {
        persistHandler.handle(message);
    }

    @Override
    protected void logFailure(Exception ex, InteractionPersistMessage message) {
        log.error("interaction persist failed messageId={}", message.getMessageId(), ex);
    }

    @Override
    protected boolean requeueOnFailure() {
        return true;
    }
}
