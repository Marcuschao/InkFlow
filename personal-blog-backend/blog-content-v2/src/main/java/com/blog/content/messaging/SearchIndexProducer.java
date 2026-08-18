package com.blog.content.messaging;

import com.blog.content.config.properties.SearchProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class SearchIndexProducer {

    private final RabbitTemplate rabbitTemplate;
    private final SearchProperties searchProperties;

    public SearchIndexProducer(RabbitTemplate rabbitTemplate, SearchProperties searchProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.searchProperties = searchProperties;
    }

    public void send(Long articleId, SearchSyncEventType eventType, LocalDateTime articleUpdatedAt) {
        if (articleId == null || eventType == null) {
            return;
        }
        SearchSyncMessage message = new SearchSyncMessage(
                UUID.randomUUID().toString(), articleId, eventType, articleUpdatedAt);
        CorrelationData correlationData = new CorrelationData(message.getMessageId());
        rabbitTemplate.convertAndSend(
                searchProperties.getExchange(),
                searchProperties.getRoutingKey(),
                message,
                correlationData);
    }
}
