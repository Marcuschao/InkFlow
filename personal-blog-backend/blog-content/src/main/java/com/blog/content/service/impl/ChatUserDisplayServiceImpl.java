package com.blog.content.service.impl;

import com.blog.content.mapper.UserMapper;
import com.blog.content.model.entity.User;
import com.blog.content.model.entity.UserProfile;
import com.blog.content.model.vo.chat.ChatMessageVo;
import com.blog.content.model.vo.chat.ChatUserDisplayVo;
import com.blog.content.service.ChatUserDisplayService;
import com.blog.content.service.UserProfileLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatUserDisplayServiceImpl implements ChatUserDisplayService {

    private final UserMapper userMapper;
    private final UserProfileLookupService userService;

    @Override
    public Map<Long, ChatUserDisplayVo> mapDisplayByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }

        List<User> users = userMapper.selectBatchIds(ids);
        Map<Long, UserProfile> profiles = userService.mapProfilesByUserIds(ids);

        Map<Long, ChatUserDisplayVo> result = new HashMap<>();
        for (User user : users) {
            if (user == null || user.getId() == null) {
                continue;
            }
            UserProfile profile = profiles.get(user.getId());
            ChatUserDisplayVo vo = new ChatUserDisplayVo();
            vo.setUserId(user.getId());
            vo.setUsername(resolveDisplayName(user, profile));
            vo.setAvatar(resolveAvatar(user, profile));
            result.put(user.getId(), vo);
        }
        
        return result;
    }

    private static String resolveDisplayName(User user, UserProfile profile) {
        if (profile != null && StringUtils.hasText(profile.getNickname())) {
            return profile.getNickname();
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private static String resolveAvatar(User user, UserProfile profile) {
        //优先使用用户资料中的头像，如果没有则使用用户表中的头像
        return profile != null ? profile.getAvatar() : user.getAvatar();
    }

    @Override
    public void enrichMessages(List<ChatMessageVo> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<Long> userIds = messages.stream()
                .filter(Objects::nonNull)
                .map(ChatMessageVo::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, ChatUserDisplayVo> displayMap = mapDisplayByUserIds(userIds);

        for (ChatMessageVo message : messages) {
            if (message == null || message.getUserId() == null) {
                continue;
            }
            ChatUserDisplayVo display = displayMap.get(message.getUserId());
            if (display == null) {
                continue;
            }
            message.setUsername(display.getUsername());
            message.setAvatar(display.getAvatar());
        }
    }
}
