package com.blog.personalblogbackend.notification.consumer;

import com.blog.personalblogbackend.notification.NotificationMessage;
import com.blog.personalblogbackend.service.UserNotificationService;
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
@ConditionalOnProperty(name = "blog.notification.enabled", havingValue = "true", matchIfMissing = true)
public class InboxNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(InboxNotificationConsumer.class);

    private final UserNotificationService userNotificationService;

    public InboxNotificationConsumer(UserNotificationService userNotificationService) {
        this.userNotificationService = userNotificationService;
    }

    @RabbitListener(queues = "#{notificationRabbitProperties.inboxQueue}")
    public void onMessage(NotificationMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            userNotificationService.saveFromMessage(message);
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.warn("[notification] inbox consume failed: {}", ex.toString());
            channel.basicNack(tag, false, false);
        }
    }
}
