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
public class InboxNotificationConsumer extends AbstractNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(InboxNotificationConsumer.class);

    private final UserNotificationService userNotificationService;

    public InboxNotificationConsumer(UserNotificationService userNotificationService,
                                     NotificationConsumeHelper consumeHelper) {
        super(consumeHelper);
        this.userNotificationService = userNotificationService;
    }

    @RabbitListener(queues = "#{notificationRabbitProperties.inboxQueue}")
    public void onMessage(NotificationMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        consume(message, channel, tag);
    }

    @Override
    protected String queueKey() {
        return NotificationConsumeHelper.QUEUE_INBOX;
    }

    @Override
    protected void doProcess(NotificationMessage message) {
        userNotificationService.saveFromMessage(message);
    }

    @Override
    protected void logFailure(Exception ex, NotificationMessage message) {
        log.warn("[notification] inbox consume failed eventId={}: {}",
                message.getEventId(), ex.toString());
    }
}
