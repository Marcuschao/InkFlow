package com.blog.personalblogbackend.messaging;

import com.blog.personalblogbackend.config.properties.SearchProperties;
import com.blog.personalblogbackend.search.SearchIndexService;
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
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class SearchIndexConsumer {
    private static final Logger log = LoggerFactory.getLogger(SearchIndexConsumer.class);

    private final SearchIndexService searchIndexService;
    private final MqIdempotencyService idempotencyService;
    private final String queueName;

    public SearchIndexConsumer(SearchIndexService searchIndexService,
                               MqIdempotencyService idempotencyService,
                               SearchProperties searchProperties) {
        this.searchIndexService = searchIndexService;
        this.idempotencyService = idempotencyService;
        this.queueName = searchProperties.getQueue();
    }

    @RabbitListener(queues = "${blog.search.queue:search.sync.queue}")
    public void onMessage(SearchSyncMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            if (message == null || message.getMessageId() == null) {
                channel.basicAck(tag, false);
                return;
            }
            if (!idempotencyService.markIfAbsent(queueName, message.getMessageId())) {
                channel.basicAck(tag, false);
                return;
            }
            searchIndexService.sync(message.getArticleId(), message.getEventType(), message.getArticleUpdatedAt());
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.warn("[search-index] consume failed: {}", ex.toString());
            channel.basicNack(tag, false, false);
        }
    }
}
