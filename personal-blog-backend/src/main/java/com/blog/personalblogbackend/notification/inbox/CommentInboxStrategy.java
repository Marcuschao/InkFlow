package com.blog.personalblogbackend.notification.inbox;

import com.blog.personalblogbackend.model.enums.NotificationTargetType;
import com.blog.personalblogbackend.model.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CommentInboxStrategy implements InboxNotificationContentStrategy {

    private final InboxNotificationSupport support;

    public CommentInboxStrategy(InboxNotificationSupport support) {
        this.support = support;
    }

    @Override
    public NotificationType type() {
        return NotificationType.COMMENT;
    }

    @Override
    public Optional<InboxNotificationDraft> build(InboxNotificationRequest request) {
        Long recipientUserId = request.getRecipientUserId();
        if (recipientUserId == null || support.skipSelf(request.getActorUserId(), recipientUserId)) {
            return Optional.empty();
        }
        String title = InboxNotificationSupport.truncate(request.getArticleTitle(), 40);
        String content = support.resolveActorName(request.getActorUserId()) + " 评论了你的文章《" + title + "》";
        return Optional.of(new InboxNotificationDraft(
                NotificationType.COMMENT, recipientUserId, request.getActorUserId(),
                request.getArticleId(), NotificationTargetType.ARTICLE, content));
    }
}
