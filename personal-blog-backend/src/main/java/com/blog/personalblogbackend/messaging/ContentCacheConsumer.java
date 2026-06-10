package com.blog.personalblogbackend.messaging;

import com.blog.personalblogbackend.cache.ArticleCacheService;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.messaging.support.AbstractIdempotentRabbitConsumer;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ContentCacheConsumer extends AbstractIdempotentRabbitConsumer<ContentChangeMessage> {
    private static final Logger log = LoggerFactory.getLogger(ContentCacheConsumer.class);

    private final ArticleCacheService articleCacheService;
    private final ArticleMapper articleMapper;
    private final String queueName;

    public ContentCacheConsumer(ArticleCacheService articleCacheService,
                                ArticleMapper articleMapper,
                                MqIdempotencyService idempotencyService,
                                @Value("${blog.content.queue:content.cache.queue}") String queueName) {
        super(idempotencyService);
        this.articleCacheService = articleCacheService;
        this.articleMapper = articleMapper;
        this.queueName = queueName;
    }

    @RabbitListener(queues = "${blog.content.queue:content.cache.queue}")
    public void onMessage(ContentChangeMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        consume(message, channel, tag);
    }

    @Override
    protected String idempotencyQueue() {
        return queueName;
    }

    @Override
    protected String extractMessageId(ContentChangeMessage message) {
        return message != null ? message.getMessageId() : null;
    }

    @Override
    protected void doProcess(ContentChangeMessage message) {
        if (message.getEventType() == ContentChangeEventType.ARTICLE_DELETED) {
            articleCacheService.delayDoubleDelete(message.getArticleId());
        } else if (message.getEventType() == ContentChangeEventType.COMMENT_APPROVED) {
            articleMapper.incrementCommentCount(message.getArticleId());
            articleCacheService.evictArticle(message.getArticleId());
        } else {
            articleCacheService.evictArticle(message.getArticleId());
        }
    }

    @Override
    protected void logFailure(Exception ex, ContentChangeMessage message) {
        log.warn("[content-cache] consume failed: {}", ex.toString());
    }
}
