package com.blog.content.model.dto.notification;

import lombok.Data;

@Data
public class NotificationQueueStatDto {
    private String name;
    private long messageCount;
    private long consumerCount;
    private long dlqMessageCount;
}
