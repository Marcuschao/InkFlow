package com.blog.personalblogbackend.config.websocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.websocket")
public class WebSocketProperties {
    private String endpoint = "/ws";
    private long heartbeat = 10000L;
}
