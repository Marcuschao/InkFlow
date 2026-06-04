package com.blog.personalblogbackend.notification.consumer;

import com.blog.personalblogbackend.notification.NotificationConsumeHelper;
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
    private final NotificationConsumeHelper consumeHelper;

    public InboxNotificationConsumer(UserNotificationService userNotificationService,
                                     NotificationConsumeHelper consumeHelper) {
        this.userNotificationService = userNotificationService;
        this.consumeHelper = consumeHelper;
    }

    @RabbitListener(queues = "#{notificationRabbitProperties.inboxQueue}")
    public void onMessage(NotificationMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        NotificationConsumeHelper.ConsumeDecision decision =
                consumeHelper.prepare(NotificationConsumeHelper.QUEUE_INBOX, message);
        if (decision != NotificationConsumeHelper.ConsumeDecision.PROCEED) {
            channel.basicAck(tag, false);
            return;
        }
        try {
            userNotificationService.saveFromMessage(message);
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.warn("[notification] inbox consume failed eventId={}: {}",
                    message.getEventId(), ex.toString());
            channel.basicNack(tag, false, false);
        }
    }
}
