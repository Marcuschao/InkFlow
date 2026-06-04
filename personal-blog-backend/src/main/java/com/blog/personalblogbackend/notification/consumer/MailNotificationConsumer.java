package com.blog.personalblogbackend.notification.consumer;

import com.blog.personalblogbackend.notification.NotificationConsumeHelper;
import com.blog.personalblogbackend.notification.NotificationMessage;
import com.blog.personalblogbackend.service.BlogMailService;
import com.blog.personalblogbackend.service.BlogSiteService;
import com.blog.personalblogbackend.service.SubscriberService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "blog.notification.enabled", havingValue = "true", matchIfMissing = true)
public class MailNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(MailNotificationConsumer.class);

    private final SubscriberService subscriberService;
    private final BlogMailService blogMailService;
    private final BlogSiteService blogSiteService;
    private final NotificationConsumeHelper consumeHelper;

    public MailNotificationConsumer(SubscriberService subscriberService,
                                    BlogMailService blogMailService,
                                    BlogSiteService blogSiteService,
                                    NotificationConsumeHelper consumeHelper) {
        this.subscriberService = subscriberService;
        this.blogMailService = blogMailService;
        this.blogSiteService = blogSiteService;
        this.consumeHelper = consumeHelper;
    }

    @RabbitListener(queues = "#{notificationRabbitProperties.mailQueue}")
    public void onMessage(NotificationMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        NotificationConsumeHelper.ConsumeDecision decision =
                consumeHelper.prepare(NotificationConsumeHelper.QUEUE_MAIL, message);
        if (decision != NotificationConsumeHelper.ConsumeDecision.PROCEED) {
            channel.basicAck(tag, false);
            return;
        }
        try {
            List<String> emails = subscriberService.listActiveEmails();
            Map<String, Object> p = message.getPayload();
            if (!emails.isEmpty() && p != null) {
                Long articleId = toLong(p.get("articleId"));
                String title = p.get("title") != null ? String.valueOf(p.get("title")) : "";
                String summary = p.get("summary") != null ? String.valueOf(p.get("summary")) : "";
                if (articleId != null) {
                    String url = blogSiteService.resolvePublicUrl("/article/" + articleId);
                    String subject = "[" + blogSiteService.getSiteTitle() + "] 新文章：" + title;
                    String body = title + "\n\n" + summary + "\n\n阅读：" + url + "\n";
                    for (String email : emails) {
                        blogMailService.sendIfConfigured(email, subject, body);
                    }
                }
            }
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.warn("[notification] mail consume failed eventId={}: {}",
                    message.getEventId(), ex.toString());
            channel.basicNack(tag, false, false);
        }
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
