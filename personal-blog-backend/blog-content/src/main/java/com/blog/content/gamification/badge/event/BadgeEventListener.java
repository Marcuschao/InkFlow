package com.blog.content.gamification.badge.event;

import com.blog.content.gamification.badge.service.BadgeService;
import com.blog.content.gamification.event.UserActivityEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class BadgeEventListener {

    private final BadgeService badgeService;

    public BadgeEventListener(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @Async
    @EventListener
    public void onUserActivity(UserActivityEvent event) {
        badgeService.checkAndAward(event.getUserId(), event.getType());
    }
}
