package com.blog.personalblogbackend.messaging;

import com.blog.personalblogbackend.cache.ArticleCacheService;
import com.blog.personalblogbackend.mapper.ArticleMapper;
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
public class ContentCacheConsumer {
    private static final Logger log = LoggerFactory.getLogger(ContentCacheConsumer.class);

    private final ArticleCacheService articleCacheService;
    private final ArticleMapper articleMapper;
    private final MqIdempotencyService idempotencyService;
    private final String queueName;

    public ContentCacheConsumer(ArticleCacheService articleCacheService,
                                ArticleMapper articleMapper,
                                MqIdempotencyService idempotencyService,
                                @Value("${blog.content.queue:content.cache.queue}") String queueName) {
        this.articleCacheService = articleCacheService;
        this.articleMapper = articleMapper;
        this.idempotencyService = idempotencyService;
        this.queueName = queueName;
    }

    @RabbitListener(queues = "${blog.content.queue:content.cache.queue}")
    public void onMessage(ContentChangeMessage message, Channel channel,
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
            if (message.getEventType() == ContentChangeEventType.ARTICLE_DELETED) {
                articleCacheService.delayDoubleDelete(message.getArticleId());
            } else if (message.getEventType() == ContentChangeEventType.COMMENT_APPROVED) {
                articleMapper.incrementCommentCount(message.getArticleId());
                articleCacheService.evictArticle(message.getArticleId());
            } else {
                articleCacheService.evictArticle(message.getArticleId());
            }
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.warn("[content-cache] consume failed: {}", ex.toString());
            channel.basicNack(tag, false, false);
        }
    }
}
