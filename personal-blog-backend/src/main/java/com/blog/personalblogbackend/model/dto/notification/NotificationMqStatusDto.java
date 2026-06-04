package com.blog.personalblogbackend.model.dto.notification;

import lombok.Data;

import java.util.List;

@Data
public class NotificationMqStatusDto {
    private boolean enabled;
    private boolean connected;
    private String exchange;
    private List<String> queues;
    private List<NotificationQueueStatDto> queueStats;
}
