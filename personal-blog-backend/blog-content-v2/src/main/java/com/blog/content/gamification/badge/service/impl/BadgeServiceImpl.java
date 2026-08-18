package com.blog.content.gamification.badge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.gamification.badge.mapper.BadgeMapper;
import com.blog.content.gamification.badge.mapper.UserBadgeMapper;
import com.blog.content.gamification.badge.model.BadgeTriggerCondition;
import com.blog.content.gamification.badge.model.entity.Badge;
import com.blog.content.gamification.badge.model.entity.UserBadge;
import com.blog.content.gamification.badge.model.vo.BadgeListVo;
import com.blog.content.gamification.badge.model.vo.BadgeVo;
import com.blog.content.gamification.badge.service.BadgeService;
import com.blog.content.gamification.event.ActivityType;
import com.blog.content.gamification.points.service.PointsService;
import com.blog.content.gamification.sign.service.SignService;
import com.blog.content.mapper.ArticleMapper;
import com.blog.content.mapper.CommentMapper;
import com.blog.content.mapper.UserProfileMapper;
import com.blog.content.model.entity.Article;
import com.blog.content.model.entity.Comment;
import com.blog.content.model.entity.UserProfile;
import com.blog.content.model.enums.ArticleStatus;
import com.blog.content.service.impl.CommentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BadgeServiceImpl implements BadgeService {

    private final BadgeMapper badgeMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final UserProfileMapper userProfileMapper;
    private final PointsService pointsService;
    private final SignService signService;
    private final ObjectMapper objectMapper;

    public BadgeServiceImpl(BadgeMapper badgeMapper,
                            UserBadgeMapper userBadgeMapper,
                            ArticleMapper articleMapper,
                            CommentMapper commentMapper,
                            UserProfileMapper userProfileMapper,
                            PointsService pointsService,
                            SignService signService,
                            ObjectMapper objectMapper) {
        this.badgeMapper = badgeMapper;
        this.userBadgeMapper = userBadgeMapper;
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.userProfileMapper = userProfileMapper;
        this.pointsService = pointsService;
        this.signService = signService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void checkAndAward(Long userId, ActivityType activityType) {
        if (userId == null || activityType == null) {
            return;
        }
        List<Badge> badges = badgeMapper.selectList(null);
        Set<Long> earned = loadEarnedBadgeIds(userId);
        for (Badge badge : badges) {
            if (earned.contains(badge.getId())) {
                continue;
            }
            BadgeTriggerCondition cond = parseCondition(badge.getTriggerCondition());
            if (cond == null || !matchesActivity(cond.getType(), activityType)) {
                continue;
            }
            if (isEligible(userId, cond, activityType)) {
                award(userId, badge.getId());
            }
        }
    }

    @Override
    public BadgeListVo listAllWithEarned(Long viewerUserId) {
        List<Badge> all = badgeMapper.selectList(new LambdaQueryWrapper<Badge>().orderByAsc(Badge::getId));
        Set<Long> earnedIds = viewerUserId != null ? loadEarnedBadgeIds(viewerUserId) : Set.of();
        Map<Long, LocalDateTime> awardedTimes = loadAwardedTimes(viewerUserId);
        List<BadgeVo> vos = all.stream()
                .map(b -> toVo(b, earnedIds.contains(b.getId()), awardedTimes.get(b.getId())))
                .collect(Collectors.toList());
        return new BadgeListVo(vos, new ArrayList<>(earnedIds));
    }

    @Override
    public List<BadgeVo> listUserBadges(Long userId) {
        List<UserBadge> rows = userBadgeMapper.selectList(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .orderByDesc(UserBadge::getAwardedTime));
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> badgeIds = rows.stream().map(UserBadge::getBadgeId).collect(Collectors.toList());
        List<Badge> badgeList = badgeMapper.selectList(new LambdaQueryWrapper<Badge>().in(Badge::getId, badgeIds));
        Map<Long, Badge> badgeMap = badgeList.stream()
                .collect(Collectors.toMap(Badge::getId, b -> b, (a, b) -> a));
        List<BadgeVo> out = new ArrayList<>();
        for (UserBadge row : rows) {
            Badge badge = badgeMap.get(row.getBadgeId());
            if (badge != null) {
                out.add(toVo(badge, true, row.getAwardedTime()));
            }
        }
        return out;
    }

    @Override
    public List<BadgeVo> listUserBadgesTop(Long userId, int limit) {
        List<BadgeVo> all = listUserBadges(userId);
        if (all.size() <= limit) {
            return all;
        }
        return all.subList(0, limit);
    }

    private void award(Long userId, Long badgeId) {
        UserBadge row = new UserBadge();
        row.setUserId(userId);
        row.setBadgeId(badgeId);
        row.setAwardedTime(LocalDateTime.now());
        try {
            userBadgeMapper.insert(row);
        } catch (DuplicateKeyException ignored) {
        }
    }

    private Set<Long> loadEarnedBadgeIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        List<UserBadge> rows = userBadgeMapper.selectList(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId));
        return rows.stream().map(UserBadge::getBadgeId).collect(Collectors.toSet());
    }

    private Map<Long, LocalDateTime> loadAwardedTimes(Long userId) {
        if (userId == null) {
            return Map.of();
        }
        return userBadgeMapper.selectList(new LambdaQueryWrapper<UserBadge>()
                        .eq(UserBadge::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(UserBadge::getBadgeId, UserBadge::getAwardedTime, (a, b) -> a));
    }

    private BadgeVo toVo(Badge badge, boolean earned, LocalDateTime awardedTime) {
        return new BadgeVo(
                badge.getId(),
                badge.getName(),
                badge.getDescription(),
                badge.getIconUrl(),
                badge.getTriggerCondition(),
                earned,
                awardedTime
        );
    }

    private BadgeTriggerCondition parseCondition(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, BadgeTriggerCondition.class);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean matchesActivity(String type, ActivityType activityType) {
        return switch (activityType) {
            case ARTICLE_PUBLISHED -> "FIRST_ARTICLE".equals(type) || "ARTICLE_COUNT".equals(type);
            case FOLLOWER_GAINED -> "FOLLOWER_COUNT".equals(type);
            case COMMENT_APPROVED -> "COMMENT_COUNT".equals(type);
            case SIGN_COMPLETED -> "SIGN_STREAK".equals(type) || "SIGN_TOTAL".equals(type);
            case POINTS_CHANGED -> "POINTS_TOTAL".equals(type);
            case ARTICLE_VIEWS_UPDATED -> "SINGLE_ARTICLE_VIEWS".equals(type);
        };
    }

    private boolean isEligible(Long userId, BadgeTriggerCondition cond, ActivityType activityType) {
        return switch (cond.getType()) {
            case "FIRST_ARTICLE", "ARTICLE_COUNT" -> countPublishedArticles(userId) >= cond.getThreshold();
            case "SINGLE_ARTICLE_VIEWS" -> maxArticleViews(userId) >= cond.getThreshold();
            case "FOLLOWER_COUNT" -> followerCount(userId) >= cond.getThreshold();
            case "COMMENT_COUNT" -> countApprovedComments(userId) >= cond.getThreshold();
            case "SIGN_STREAK" -> signService.calcStreakDays(userId) >= cond.getThreshold();
            case "SIGN_TOTAL" -> signService.calcTotalDays(userId) >= cond.getThreshold();
            case "POINTS_TOTAL" -> pointsService.getTotalPoints(userId) >= cond.getThreshold();
            default -> false;
        };
    }

    private long countPublishedArticles(Long userId) {
        return articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, userId)
                .eq(Article::getStatus, ArticleStatus.PUBLISHED));
    }

    private int maxArticleViews(Long userId) {
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, userId)
                .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                .orderByDesc(Article::getViewCount)
                .last("LIMIT 1"));
        if (articles.isEmpty()) {
            return 0;
        }
        Integer vc = articles.get(0).getViewCount();
        return vc != null ? vc : 0;
    }

    private int followerCount(Long userId) {
        UserProfile profile = userProfileMapper.selectById(userId);
        return profile != null && profile.getFollowerCount() != null ? profile.getFollowerCount() : 0;
    }

    private long countApprovedComments(Long userId) {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .eq(Comment::getStatus, CommentServiceImpl.STATUS_APPROVED));
    }
}
