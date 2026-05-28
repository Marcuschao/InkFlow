package com.blog.personalblogbackend.config.websocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.chat")
public class ChatProperties {
    private boolean guestReadonly = true;
    private int historyLimit = 50;
    private int historyPageSize = 30;
    private int onlineTtlSeconds = 45;
    private int archiveHotDays = 7;
    private int archiveBatchSize = 1000;
    private String archiveCron = "0 0 1 * * ?";
}
