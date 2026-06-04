package com.blog.personalblogbackend.notification;

import com.blog.personalblogbackend.config.properties.NotificationRabbitProperties;
import com.blog.personalblogbackend.mapper.UserMapper;
import com.blog.personalblogbackend.mapper.UserProfileMapper;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.User;
import com.blog.personalblogbackend.model.entity.UserProfile;
import com.blog.personalblogbackend.model.enums.NotificationTargetType;
import com.blog.personalblogbackend.model.enums.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    private final NotificationRabbitProperties props;
    private final RabbitTemplate rabbitTemplate;
    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;

    public NotificationProducer(NotificationRabbitProperties props,
                                RabbitTemplate rabbitTemplate,
                                UserProfileMapper userProfileMapper,
                                UserMapper userMapper) {
        this.props = props;
        this.rabbitTemplate = rabbitTemplate;
        this.userProfileMapper = userProfileMapper;
        this.userMapper = userMapper;
    }

    public void notifyLike(Long actorUserId, Article article) {
        if (article == null || article.getAuthorId() == null) {
            return;
        }
        Long recipient = article.getAuthorId();
        if (skipSelf(actorUserId, recipient)) {
            return;
        }
        String title = truncate(article.getTitle(), 40);
        String content = resolveActorName(actorUserId) + " 赞了你的文章《" + title + "》";
        sendInbox(NotificationType.LIKE, recipient, actorUserId, article.getId(),
                NotificationTargetType.ARTICLE, content);
    }

    public void notifyFavorite(Long actorUserId, Article article) {
        if (article == null || article.getAuthorId() == null) {
            return;
        }
        Long recipient = article.getAuthorId();
        if (skipSelf(actorUserId, recipient)) {
            return;
        }
        String title = truncate(article.getTitle(), 40);
        String content = resolveActorName(actorUserId) + " 收藏了你的文章《" + title + "》";
        sendInbox(NotificationType.FAVORITE, recipient, actorUserId, article.getId(),
                NotificationTargetType.ARTICLE, content);
    }

    public void notifyFollow(Long actorUserId, Long followeeId) {
        if (skipSelf(actorUserId, followeeId)) {
            return;
        }
        String content = resolveActorName(actorUserId) + " 关注了你";
        sendInbox(NotificationType.FOLLOW, followeeId, actorUserId, followeeId,
                NotificationTargetType.USER, content);
    }

    public void notifyComment(Long actorUserId, Long recipientUserId, Long articleId, String articleTitle) {
        if (recipientUserId == null || skipSelf(actorUserId, recipientUserId)) {
            return;
        }
        String title = truncate(articleTitle, 40);
        String content = resolveActorName(actorUserId) + " 评论了你的文章《" + title + "》";
        sendInbox(NotificationType.COMMENT, recipientUserId, actorUserId, articleId,
                NotificationTargetType.ARTICLE, content);
    }

    public void sendArticlePublished(Article article) {
        if (article == null || article.getId() == null) {
            return;
        }
        NotificationMessage msg = new NotificationMessage();
        msg.setType(DomainEventType.ARTICLE_PUBLISHED.name());
        Map<String, Object> payload = new HashMap<>();
        payload.put("articleId", article.getId());
        payload.put("title", article.getTitle());
        payload.put("summary", article.getSummary());
        msg.setPayload(payload);
        sendAfterCommit(NotificationRabbitProperties.RK_ARTICLE_PUBLISHED, msg);
    }

    public void sendDomainEvent(DomainEvent event) {
        if (event == null || event.getType() == null) {
            return;
        }
        NotificationMessage msg = new NotificationMessage();
        msg.setType(event.getType().name());
        msg.setPayload(event.getPayload());
        Map<String, Object> payload = event.getPayload() != null ? new HashMap<>(event.getPayload()) : new HashMap<>();
        payload.put("eventId", event.getEventId());
        payload.put("occurredAt", event.getOccurredAt() != null ? event.getOccurredAt().toString() : null);
        msg.setPayload(payload);
        if (StringUtils.hasText(event.getEventId())) {
            msg.setEventId(event.getEventId());
        }
        sendAfterCommit(NotificationRabbitProperties.RK_EVENT_PREFIX + event.getType().name(), msg);
    }

    private void sendInbox(NotificationType type, Long recipientUserId, Long actorUserId,
                           Long targetId, NotificationTargetType targetType, String content) {
        NotificationMessage msg = new NotificationMessage();
        msg.setType(type.name());
        msg.setRecipientUserId(recipientUserId);
        msg.setActorUserId(actorUserId);
        msg.setTargetId(targetId);
        msg.setTargetType(targetType.name());
        msg.setContent(content);
        sendAfterCommit(routingKeyFor(type), msg);
    }

    private String routingKeyFor(NotificationType type) {
        return switch (type) {
            case LIKE -> NotificationRabbitProperties.RK_LIKE;
            case FAVORITE -> NotificationRabbitProperties.RK_FAVORITE;
            case COMMENT -> NotificationRabbitProperties.RK_COMMENT;
            case FOLLOW -> NotificationRabbitProperties.RK_FOLLOW;
        };
    }

    public void sendAfterCommit(String routingKey, NotificationMessage message) {
        if (!props.isEnabled() || message == null) {
            return;
        }
        if (!StringUtils.hasText(message.getEventId())) {
            message.setEventId(UUID.randomUUID().toString());
        }
        Runnable task = () -> {
            try {
                rabbitTemplate.convertAndSend(props.getExchange(), routingKey, message);
                log.debug("[notification] published routingKey={} eventId={}", routingKey, message.getEventId());
            } catch (Exception ex) {
                log.warn("[notification] publish failed routingKey={} eventId={}: {}",
                        routingKey, message.getEventId(), ex.toString());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private static boolean skipSelf(Long actorUserId, Long recipientUserId) {
        return actorUserId == null || recipientUserId == null || actorUserId.equals(recipientUserId);
    }

    private String resolveActorName(Long userId) {
        if (userId == null) {
            return "某用户";
        }
        UserProfile profile = userProfileMapper.selectById(userId);
        if (profile != null && StringUtils.hasText(profile.getNickname())) {
            return profile.getNickname();
        }
        User user = userMapper.selectById(userId);
        if (user != null) {
            if (StringUtils.hasText(user.getNickname())) {
                return user.getNickname();
            }
            return user.getUsername();
        }
        return "某用户";
    }

    private static String truncate(String s, int max) {
        if (!StringUtils.hasText(s)) {
            return "无标题";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
