package com.blog.content.gamification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Collections;
import java.util.Map;

@Getter
public class UserActivityEvent extends ApplicationEvent {

    private final ActivityType type;
    private final Long userId;
    private final Map<String, Object> payload;

    public UserActivityEvent(Object source, ActivityType type, Long userId, Map<String, Object> payload) {
        super(source);
        this.type = type;
        this.userId = userId;
        this.payload = payload != null ? payload : Collections.emptyMap();
    }
}
