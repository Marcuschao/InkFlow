package com.blog.content.profile.service.impl;

import com.blog.content.gamification.badge.model.vo.BadgeVo;
import com.blog.content.gamification.badge.service.BadgeService;
import com.blog.content.gamification.points.service.PointsService;
import com.blog.content.gamification.sign.model.vo.SignStatusVo;
import com.blog.content.gamification.sign.service.SignService;
import com.blog.content.profile.model.vo.SocialCardVo;
import com.blog.content.profile.model.vo.TimelineItemVo;
import com.blog.content.profile.model.vo.VisitorVo;
import com.blog.content.profile.service.SocialCardService;
import com.blog.content.profile.service.TimelineService;
import com.blog.content.profile.service.VisitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class SocialCardServiceImpl implements SocialCardService {

    private final PointsService pointsService;
    private final BadgeService badgeService;
    private final SignService signService;
    private final VisitorService visitorService;
    private final TimelineService timelineService;

    public SocialCardServiceImpl(PointsService pointsService,
                                 BadgeService badgeService,
                                 SignService signService,
                                 VisitorService visitorService,
                                 TimelineService timelineService) {
        this.pointsService = pointsService;
        this.badgeService = badgeService;
        this.signService = signService;
        this.visitorService = visitorService;
        this.timelineService = timelineService;
    }

    @Override
    public SocialCardVo getSocialCard(Long userId, Long viewerId) {
        SocialCardVo vo = new SocialCardVo();
        vo.setPoints(safePoints(userId));
        vo.setBadges(safeBadges(userId));
        vo.setTimelinePreview(safeTimeline(userId));
        if (viewerId != null && viewerId.equals(userId)) {
            vo.setSignStatus(safeSignStatus(userId));
            vo.setRecentVisitors(safeVisitors(userId));
        }
        return vo;
    }

    private int safePoints(Long userId) {
        try {
            return pointsService.getTotalPoints(userId);
        } catch (Exception e) {
            log.warn("加载用户积分失败 userId={}: {}", userId, e.getMessage());
            return 0;
        }
    }

    private List<BadgeVo> safeBadges(Long userId) {
        try {
            return badgeService.listUserBadgesTop(userId, 6);
        } catch (Exception e) {
            log.warn("加载用户徽章失败 userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<TimelineItemVo> safeTimeline(Long userId) {
        try {
            return timelineService.interactionTimeline(userId, 5);
        } catch (Exception e) {
            log.warn("加载互动时间线失败 userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private SignStatusVo safeSignStatus(Long userId) {
        try {
            return signService.status(userId);
        } catch (Exception e) {
            log.warn("加载签到状态失败 userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    private List<VisitorVo> safeVisitors(Long userId) {
        try {
            return visitorService.recentVisitors(userId, 20);
        } catch (Exception e) {
            log.warn("加载访客记录失败 userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
