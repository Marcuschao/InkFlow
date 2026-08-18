package com.blog.content.notification.inbox;

import com.blog.content.model.enums.NotificationTargetType;
import com.blog.content.model.enums.NotificationType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InboxNotificationDraft {

    private final NotificationType type;
    private final Long recipientUserId;
    private final Long actorUserId;
    private final Long targetId;
    private final NotificationTargetType targetType;
    private final String content;
}
