package com.blog.content.notification;

import com.blog.content.messaging.MqIdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NotificationConsumeHelper {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumeHelper.class);

    public static final String QUEUE_INBOX = "notification.inbox";
    public static final String QUEUE_PUSH = "notification.push";
    public static final String QUEUE_MAIL = "notification.mail";
    public static final String QUEUE_AUDIT = "notification.audit";

    private final MqIdempotencyService idempotencyService;

    public NotificationConsumeHelper(MqIdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    public ConsumeDecision prepare(String queue, NotificationMessage message) {
        if (message == null) {
            return ConsumeDecision.DISCARD;
        }
        if (!StringUtils.hasText(message.getEventId())) {
            log.warn("[notification] missing eventId on queue={}, type={}", queue, message.getType());
            return ConsumeDecision.DISCARD;
        }
        if (!idempotencyService.markIfAbsent(queue, message.getEventId())) {
            log.debug("[notification] duplicate eventId={} queue={}", message.getEventId(), queue);
            return ConsumeDecision.DUPLICATE;
        }
        return ConsumeDecision.PROCEED;
    }

    public enum ConsumeDecision {
        PROCEED,
        DUPLICATE,
        DISCARD
    }
}
