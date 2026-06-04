package com.blog.personalblogbackend.notification;

import com.blog.personalblogbackend.messaging.MqIdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class NotificationIdempotencyTest {

    @Autowired
    private MqIdempotencyService idempotencyService;

    @Test
    void markIfAbsent_onlyOncePerEventId() {
        String eventId = UUID.randomUUID().toString();
        assertTrue(idempotencyService.markIfAbsent(NotificationConsumeHelper.QUEUE_INBOX, eventId));
        assertFalse(idempotencyService.markIfAbsent(NotificationConsumeHelper.QUEUE_INBOX, eventId));
    }
}
