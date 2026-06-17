package com.blog.content.gamification.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserActivityEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public UserActivityEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(ActivityType type, Long userId, Map<String, Object> payload) {
        applicationEventPublisher.publishEvent(new UserActivityEvent(this, type, userId, payload));
    }

    public void publish(ActivityType type, Long userId) {
        publish(type, userId, Map.of());
    }
}
