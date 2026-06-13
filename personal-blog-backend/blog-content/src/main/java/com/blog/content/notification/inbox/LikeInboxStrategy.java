package com.blog.content.notification.inbox;

import com.blog.content.model.entity.Article;
import com.blog.content.model.enums.NotificationTargetType;
import com.blog.content.model.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LikeInboxStrategy implements InboxNotificationContentStrategy {

    private final InboxNotificationSupport support;

    public LikeInboxStrategy(InboxNotificationSupport support) {
        this.support = support;
    }

    @Override
    public NotificationType type() {
        return NotificationType.LIKE;
    }

    @Override
    public Optional<InboxNotificationDraft> build(InboxNotificationRequest request) {
        Article article = request.getArticle();
        if (article == null || article.getAuthorId() == null) {
            return Optional.empty();
        }
        Long recipient = article.getAuthorId();
        if (support.skipSelf(request.getActorUserId(), recipient)) {
            return Optional.empty();
        }
        String title = InboxNotificationSupport.truncate(article.getTitle(), 40);
        String content = support.resolveActorName(request.getActorUserId()) + " 赞了你的文章《" + title + "》";
        return Optional.of(new InboxNotificationDraft(
                NotificationType.LIKE, recipient, request.getActorUserId(),
                article.getId(), NotificationTargetType.ARTICLE, content));
    }
}
