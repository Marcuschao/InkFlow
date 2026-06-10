package com.blog.personalblogbackend.notification.inbox;

import com.blog.personalblogbackend.model.enums.NotificationTargetType;
import com.blog.personalblogbackend.model.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FollowInboxStrategy implements InboxNotificationContentStrategy {

    private final InboxNotificationSupport support;

    public FollowInboxStrategy(InboxNotificationSupport support) {
        this.support = support;
    }

    @Override
    public NotificationType type() {
        return NotificationType.FOLLOW;
    }

    @Override
    public Optional<InboxNotificationDraft> build(InboxNotificationRequest request) {
        Long followeeId = request.getFolloweeId();
        if (support.skipSelf(request.getActorUserId(), followeeId)) {
            return Optional.empty();
        }
        String content = support.resolveActorName(request.getActorUserId()) + " 关注了你";
        return Optional.of(new InboxNotificationDraft(
                NotificationType.FOLLOW, followeeId, request.getActorUserId(),
                followeeId, NotificationTargetType.USER, content));
    }
}
