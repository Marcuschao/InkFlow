package com.blog.personalblogbackend.messaging;

import com.blog.personalblogbackend.config.properties.SearchProperties;
import com.blog.personalblogbackend.messaging.support.AbstractIdempotentRabbitConsumer;
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
public class SearchIndexConsumer extends AbstractIdempotentRabbitConsumer<SearchSyncMessage> {
    private static final Logger log = LoggerFactory.getLogger(SearchIndexConsumer.class);

    private final SearchIndexService searchIndexService;
    private final String queueName;

    public SearchIndexConsumer(SearchIndexService searchIndexService,
                               MqIdempotencyService idempotencyService,
                               SearchProperties searchProperties) {
        super(idempotencyService);
        this.searchIndexService = searchIndexService;
        this.queueName = searchProperties.getQueue();
    }

    @RabbitListener(queues = "${blog.search.queue:search.sync.queue}")
    public void onMessage(SearchSyncMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        consume(message, channel, tag);
    }

    @Override
    protected String idempotencyQueue() {
        return queueName;
    }

    @Override
    protected String extractMessageId(SearchSyncMessage message) {
        return message != null ? message.getMessageId() : null;
    }

    @Override
    protected void doProcess(SearchSyncMessage message) {
        searchIndexService.sync(message.getArticleId(), message.getEventType(), message.getArticleUpdatedAt());
    }

    @Override
    protected void logFailure(Exception ex, SearchSyncMessage message) {
        log.warn("[search-index] consume failed: {}", ex.toString());
    }
}
