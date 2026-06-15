package com.blog.content.knowledge.service.impl;

import com.blog.content.common.exception.ServiceException;
import com.blog.content.knowledge.service.KnowledgeSubscriptionService;
import com.blog.content.mapper.UserTagSubscriptionMapper;
import com.blog.content.model.entity.UserNotification;
import com.blog.content.model.entity.UserTagSubscription;
import com.blog.content.mapper.UserNotificationMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeSubscriptionServiceImpl implements KnowledgeSubscriptionService {

    private final UserTagSubscriptionMapper subscriptionMapper;
    private final UserNotificationMapper notificationMapper;

    public KnowledgeSubscriptionServiceImpl(UserTagSubscriptionMapper subscriptionMapper,
                                              UserNotificationMapper notificationMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Transactional
    public void subscribe(Long userId, Long tagId) {
        if (userId == null || tagId == null) {
            throw new ServiceException(400, "参数无效");
        }
        Long cnt = subscriptionMapper.selectCount(new QueryWrapper<UserTagSubscription>()
                .eq("user_id", userId).eq("tag_id", tagId));
        if (cnt != null && cnt > 0) {
            return;
        }
        UserTagSubscription sub = new UserTagSubscription();
        sub.setUserId(userId);
        sub.setTagId(tagId);
        sub.setCreateTime(LocalDateTime.now());
        subscriptionMapper.insert(sub);
    }

    @Override
    @Transactional
    public void unsubscribe(Long userId, Long tagId) {
        subscriptionMapper.delete(new QueryWrapper<UserTagSubscription>()
                .eq("user_id", userId).eq("tag_id", tagId));
    }

    @Override
    public boolean isSubscribed(Long userId, Long tagId) {
        if (userId == null || tagId == null) {
            return false;
        }
        Long cnt = subscriptionMapper.selectCount(new QueryWrapper<UserTagSubscription>()
                .eq("user_id", userId).eq("tag_id", tagId));
        return cnt != null && cnt > 0;
    }

    @Override
    public void notifySubscribers(Long articleId, List<Long> tagIds, String title) {
        if (articleId == null || tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            List<Long> userIds = subscriptionMapper.selectUserIdsByTagId(tagId);
            if (userIds == null) {
                continue;
            }
            for (Long uid : userIds) {
                UserNotification n = new UserNotification();
                n.setRecipientUserId(uid);
                n.setType("TAG_ARTICLE");
                n.setContent("你订阅的标签有新文章：" + (title != null ? title : ""));
                n.setTargetId(articleId);
                n.setTargetType("ARTICLE");
                n.setIsRead(0);
                n.setCreateTime(LocalDateTime.now());
                notificationMapper.insert(n);
            }
        }
    }
}
