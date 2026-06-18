package com.blog.content.notification.inbox;

import com.blog.content.model.entity.Article;
import com.blog.content.model.enums.NotificationTargetType;
import com.blog.content.model.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RewardInboxStrategy implements InboxNotificationContentStrategy {

    private final InboxNotificationSupport support;

    public RewardInboxStrategy(InboxNotificationSupport support) {
        this.support = support;
    }

    @Override
    public NotificationType type() {
        return NotificationType.REWARD;
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
        int points = request.getPoints() != null ? request.getPoints() : 0;
        String content = support.resolveActorName(request.getActorUserId()) + " 打赏了你的文章《" + title + "》" + points + " 积分";
        return Optional.of(new InboxNotificationDraft(
                NotificationType.REWARD, recipient, request.getActorUserId(),
                article.getId(), NotificationTargetType.ARTICLE, content));
    }
}
