package com.blog.personalblogbackend.notification.consumer;

import com.blog.personalblogbackend.notification.NotificationConsumeHelper;
import com.blog.personalblogbackend.notification.NotificationMessage;
import com.blog.personalblogbackend.service.WebPushService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "blog.notification.enabled", havingValue = "true", matchIfMissing = true)
public class PushNotificationConsumer extends AbstractNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationConsumer.class);

    private final WebPushService webPushService;

    public PushNotificationConsumer(WebPushService webPushService, NotificationConsumeHelper consumeHelper) {
        super(consumeHelper);
        this.webPushService = webPushService;
    }

    @RabbitListener(queues = "#{notificationRabbitProperties.pushQueue}")
    public void onMessage(NotificationMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        consume(message, channel, tag);
    }

    @Override
    protected String queueKey() {
        return NotificationConsumeHelper.QUEUE_PUSH;
    }

    @Override
    protected void doProcess(NotificationMessage message) {
        Map<String, Object> p = message.getPayload();
        if (p != null) {
            Long articleId = toLong(p.get("articleId"));
            String title = p.get("title") != null ? String.valueOf(p.get("title")) : null;
            if (articleId != null) {
                webPushService.notifyNewArticle(articleId, title);
            }
        }
    }

    @Override
    protected void logFailure(Exception ex, NotificationMessage message) {
        log.warn("[notification] push consume failed eventId={}: {}",
                message.getEventId(), ex.toString());
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }
}
