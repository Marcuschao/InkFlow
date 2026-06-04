package com.blog.personalblogbackend.notification;

import lombok.Data;

import java.util.Map;

@Data
public class NotificationMessage {
    private String eventId;
    private String type;
    private Long recipientUserId;
    private Long actorUserId;
    private Long targetId;
    private String targetType;
    private String content;
    private Map<String, Object> payload;
}
