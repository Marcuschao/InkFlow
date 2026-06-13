package com.blog.content.notification.inbox;

import com.blog.content.mapper.UserMapper;
import com.blog.content.mapper.UserProfileMapper;
import com.blog.content.model.entity.User;
import com.blog.content.model.entity.UserProfile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InboxNotificationSupport {

    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;

    public InboxNotificationSupport(UserProfileMapper userProfileMapper, UserMapper userMapper) {
        this.userProfileMapper = userProfileMapper;
        this.userMapper = userMapper;
    }

    public boolean skipSelf(Long actorUserId, Long recipientUserId) {
        return actorUserId == null || recipientUserId == null || actorUserId.equals(recipientUserId);
    }

    public String resolveActorName(Long userId) {
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

    public static String truncate(String s, int max) {
        if (!StringUtils.hasText(s)) {
            return "无标题";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
