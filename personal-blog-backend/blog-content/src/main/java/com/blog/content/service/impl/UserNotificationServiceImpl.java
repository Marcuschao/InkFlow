package com.blog.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.common.exception.ServiceException;
import com.blog.content.common.support.PageResult;
import com.blog.content.mapper.UserNotificationMapper;
import com.blog.content.model.entity.UserNotification;
import com.blog.content.model.entity.UserProfile;
import com.blog.content.model.vo.notification.NotificationVo;
import com.blog.content.notification.NotificationMessage;
import com.blog.content.notification.RealtimeNotificationService;
import com.blog.content.service.UserNotificationService;
import com.blog.content.service.UserProfileLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserNotificationServiceImpl implements UserNotificationService {

    private static final Logger log = LoggerFactory.getLogger(UserNotificationServiceImpl.class);

    @Autowired
    private UserNotificationMapper userNotificationMapper;
    @Autowired
    private UserProfileLookupService userService;
    @Autowired
    private RealtimeNotificationService realtimeNotificationService;

    @Override
    public PageResult<NotificationVo> pageForUser(Long userId, int page, int size) {
        Page<UserNotification> mp = userNotificationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getRecipientUserId, userId)
                        .orderByDesc(UserNotification::getCreateTime));
        Set<Long> actorIds = mp.getRecords().stream()
                .map(UserNotification::getActorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserProfile> profileMap = userService.mapProfilesByUserIds(actorIds);
        List<NotificationVo> vos = mp.getRecords().stream()
                .map(n -> toVo(n, profileMap.get(n.getActorUserId())))
                .collect(Collectors.toList());
        return PageResult.build(vos, mp.getTotal(), mp.getSize(), mp.getCurrent());
    }

    @Override
    public long unreadCount(Long userId) {
        Long c = userNotificationMapper.selectCount(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getRecipientUserId, userId)
                .eq(UserNotification::getIsRead, 0));
        return c != null ? c : 0;
    }

    @Override
    public void markRead(Long userId, Long notificationId) {
        UserNotification n = requireOwned(userId, notificationId);
        if (n.getIsRead() != null && n.getIsRead() == 1) {
            return;
        }
        n.setIsRead(1);
        userNotificationMapper.updateById(n);
    }

    @Override
    public void markAllRead(Long userId) {
        userNotificationMapper.update(null, new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getRecipientUserId, userId)
                .eq(UserNotification::getIsRead, 0)
                .set(UserNotification::getIsRead, 1));
    }

    @Override
    public void delete(Long userId, Long notificationId) {
        UserNotification n = requireOwned(userId, notificationId);
        userNotificationMapper.deleteById(n.getId());
    }

    @Override
    public void saveFromMessage(NotificationMessage message) {
        if (message == null || message.getRecipientUserId() == null) {
            return;
        }
        if (message.getActorUserId() != null
                && message.getActorUserId().equals(message.getRecipientUserId())) {
            return;
        }
        if (!StringUtils.hasText(message.getContent())) {
            return;
        }
        UserNotification n = new UserNotification();
        n.setRecipientUserId(message.getRecipientUserId());
        n.setActorUserId(message.getActorUserId());
        n.setType(message.getType());
        n.setTargetId(message.getTargetId());
        n.setTargetType(message.getTargetType());
        n.setContent(message.getContent().trim());
        n.setEventId(message.getEventId());
        n.setIsRead(0);
        n.setCreateTime(LocalDateTime.now());
        try {
            userNotificationMapper.insert(n);
        } catch (DuplicateKeyException ex) {
            log.debug("[notification] duplicate inbox eventId={} recipient={}",
                    message.getEventId(), message.getRecipientUserId());
            return;
        }
        UserProfile actorProfile = null;
        if (message.getActorUserId() != null) {
            actorProfile = userService.mapProfilesByUserIds(Set.of(message.getActorUserId()))
                    .get(message.getActorUserId());
        }
        NotificationVo vo = toVo(n, actorProfile);
        realtimeNotificationService.pushToUser(message.getRecipientUserId(), vo);
    }

    private UserNotification requireOwned(Long userId, Long notificationId) {
        UserNotification n = userNotificationMapper.selectById(notificationId);
        if (n == null || !userId.equals(n.getRecipientUserId())) {
            throw new ServiceException(404, "通知不存在");
        }
        return n;
    }

    private NotificationVo toVo(UserNotification n, UserProfile profile) {
        NotificationVo vo = new NotificationVo();
        vo.setId(n.getId());
        vo.setActorUserId(n.getActorUserId());
        vo.setType(n.getType());
        vo.setTargetId(n.getTargetId());
        vo.setTargetType(n.getTargetType());
        vo.setContent(n.getContent());
        vo.setRead(n.getIsRead() != null && n.getIsRead() == 1);
        vo.setCreateTime(n.getCreateTime());
        if (profile != null) {
            vo.setActorNickname(profile.getNickname());
            vo.setActorAvatar(profile.getAvatar());
        }
        return vo;
    }
}
