package com.blog.content.gamification.badge.service;

import com.blog.content.gamification.badge.model.vo.BadgeListVo;
import com.blog.content.gamification.badge.model.vo.BadgeVo;
import com.blog.content.gamification.event.ActivityType;

import java.util.List;

public interface BadgeService {

    void checkAndAward(Long userId, ActivityType activityType);

    BadgeListVo listAllWithEarned(Long viewerUserId);

    List<BadgeVo> listUserBadges(Long userId);

    List<BadgeVo> listUserBadgesTop(Long userId, int limit);
}
