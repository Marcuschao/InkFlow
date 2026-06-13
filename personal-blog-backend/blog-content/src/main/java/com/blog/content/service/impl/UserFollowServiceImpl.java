package com.blog.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.common.exception.ServiceException;
import com.blog.content.common.support.PageResult;
import com.blog.content.mapper.UserFollowMapper;
import com.blog.content.mapper.UserMapper;
import com.blog.content.mapper.UserProfileMapper;
import com.blog.content.model.entity.User;
import com.blog.content.model.entity.UserFollow;
import com.blog.content.model.entity.UserProfile;
import com.blog.content.model.vo.interaction.FollotatusVo;
import com.blog.content.model.vo.interaction.FollowToggleVo;
import com.blog.content.model.vo.interaction.UserBriefVo;
import com.blog.content.notification.NotificationProducer;
import com.blog.content.service.UserFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserFollowServiceImpl implements UserFollowService {

    @Autowired
    private UserFollowMapper userFollowMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserProfileMapper userProfileMapper;
    @Autowired
    private NotificationProducer notificationProducer;

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }
        return user;
    }

    private int countOrZero(Integer n) {
        return n != null ? n : 0;
    }

    private void ensureProfile(Long userId) {
        if (userProfileMapper.selectById(userId) != null) {
            return;
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            return;
        }
        UserProfile p = new UserProfile();
        p.setUserId(userId);
        p.setNickname(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
        p.setFollowerCount(0);
        p.setFollowingCount(0);
        p.setUpdateTime(LocalDateTime.now());
        userProfileMapper.insert(p);
    }

    private void bumpCounts(Long followerId, Long followeeId, int delta) {
        ensureProfile(followerId);
        ensureProfile(followeeId);
        UserProfile follower = userProfileMapper.selectById(followerId);
        UserProfile followee = userProfileMapper.selectById(followeeId);
        follower.setFollowingCount(Math.max(0, countOrZero(follower.getFollowingCount()) + delta));
        follower.setUpdateTime(LocalDateTime.now());
        followee.setFollowerCount(Math.max(0, countOrZero(followee.getFollowerCount()) + delta));
        followee.setUpdateTime(LocalDateTime.now());
        userProfileMapper.updateById(follower);
        userProfileMapper.updateById(followee);
    }

    @Override
    @Transactional
    public FollowToggleVo toggle(Long followerId, Long followeeId) {
        requireUser(followeeId);
        if (followerId.equals(followeeId)) {
            throw new ServiceException(400, "不能关注自己");
        }
        boolean exists = status(followerId, followeeId).getFollowing();
        if (exists) {
            userFollowMapper.delete(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, followerId)
                    .eq(UserFollow::getFolloweeId, followeeId));
            bumpCounts(followerId, followeeId, -1);
        } else {
            UserFollow row = new UserFollow();
            row.setFollowerId(followerId);
            row.setFolloweeId(followeeId);
            row.setCreateTime(LocalDateTime.now());
            userFollowMapper.insert(row);
            bumpCounts(followerId, followeeId, 1);
            notificationProducer.notifyFollow(followerId, followeeId);
        }
        UserProfile target = userProfileMapper.selectById(followeeId);
        UserProfile self = userProfileMapper.selectById(followerId);
        return new FollowToggleVo(
                !exists,
                target != null ? countOrZero(target.getFollowerCount()) : 0,
                self != null ? countOrZero(self.getFollowingCount()) : 0
        );
    }

    @Override
    public FollotatusVo status(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return new FollotatusVo(false);
        }
        long c = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId));
        return new FollotatusVo(c > 0);
    }

    @Override
    public PageResult<UserBriefVo> followers(Long userId, Long viewerId, int page, int size) {
        requireUser(userId);
        long offset = (long) (page - 1) * size;
        List<Long> ids = userFollowMapper.selectFollowerIds(userId, offset, size);
        long total = userFollowMapper.countFollowers(userId);
        return PageResult.build(toBriefList(ids, viewerId), total, size, page);
    }

    @Override
    public PageResult<UserBriefVo> following(Long userId, Long viewerId, int page, int size) {
        requireUser(userId);
        long offset = (long) (page - 1) * size;
        List<Long> ids = userFollowMapper.selectFollowingIds(userId, offset, size);
        long total = userFollowMapper.countFollowing(userId);
        return PageResult.build(toBriefList(ids, viewerId), total, size, page);
    }

    private List<UserBriefVo> toBriefList(List<Long> ids, Long viewerId) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<UserProfile> profiles = userProfileMapper.selectList(
                new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, ids));
        Map<Long, UserProfile> map = profiles.stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p, (a, b) -> a));
        Set<Long> followingSet = new HashSet<>();
        if (viewerId != null) {
            List<UserFollow> rows = userFollowMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, viewerId)
                    .in(UserFollow::getFolloweeId, ids));
            followingSet = rows.stream().map(UserFollow::getFolloweeId).collect(Collectors.toSet());
        }
        List<UserBriefVo> out = new ArrayList<>();
        for (Long id : ids) {
            UserProfile p = map.get(id);
            User u = userMapper.selectById(id);
            String nickname = p != null && StringUtils.hasText(p.getNickname())
                    ? p.getNickname()
                    : (u != null ? u.getUsername() : "用户");
            String avatar = p != null ? p.getAvatar() : null;
            out.add(new UserBriefVo(id, nickname, avatar, followingSet.contains(id)));
        }
        return out;
    }
}
