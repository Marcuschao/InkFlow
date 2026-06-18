package com.blog.content.gamification.points.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.common.exception.ServiceException;
import com.blog.content.gamification.event.ActivityType;
import com.blog.content.gamification.event.UserActivityEventPublisher;
import com.blog.content.gamification.points.mapper.PointsLogMapper;
import com.blog.content.gamification.points.model.entity.PointsLog;
import com.blog.content.gamification.points.model.vo.PointsVo;
import com.blog.content.gamification.points.service.PointsService;
import com.blog.content.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PointsServiceImpl implements PointsService {

    private final UserMapper userMapper;
    private final PointsLogMapper pointsLogMapper;
    private final UserActivityEventPublisher activityEventPublisher;

    public PointsServiceImpl(UserMapper userMapper,
                             PointsLogMapper pointsLogMapper,
                             UserActivityEventPublisher activityEventPublisher) {
        this.userMapper = userMapper;
        this.pointsLogMapper = pointsLogMapper;
        this.activityEventPublisher = activityEventPublisher;
    }

    @Override
    @Transactional
    public void addPoints(Long userId, int delta, String reason) {
        if (userId == null || delta == 0) {
            return;
        }
        if (userMapper.selectById(userId) == null) {
            return;
        }
        int current = getTotalPoints(userId);
        int next = Math.max(0, current + delta);
        try {
            userMapper.addPointsById(userId, delta);
        } catch (Exception ignored) {
            return;
        }

        PointsLog log = new PointsLog();
        log.setUserId(userId);
        log.setPointsChange(delta);
        log.setReason(reason);
        log.setCreateTime(LocalDateTime.now());
        try {
            pointsLogMapper.insert(log);
        } catch (Exception ignored) {
        }

        activityEventPublisher.publish(ActivityType.POINTS_CHANGED, userId, Map.of("points", next));
    }

    @Override
    @Transactional
    public boolean deductPoints(Long userId, int amount, String reason) {
        if (userId == null || amount <= 0) {
            throw new ServiceException(400, "积分数量不正确");
        }
        int updated = userMapper.deductPointsIfEnough(userId, amount);
        if (updated <= 0) {
            return false;
        }
        insertLog(userId, -amount, reason);
        activityEventPublisher.publish(ActivityType.POINTS_CHANGED, userId, Map.of("points", getTotalPoints(userId)));
        return true;
    }

    @Override
    @Transactional
    public void transferPoints(Long fromUserId, Long toUserId, int amount, String outReason, String inReason) {
        if (fromUserId == null || toUserId == null || amount <= 0) {
            throw new ServiceException(400, "积分数量不正确");
        }
        if (fromUserId.equals(toUserId)) {
            throw new ServiceException(400, "不能给自己打赏");
        }
        int updated = userMapper.deductPointsIfEnough(fromUserId, amount);
        if (updated <= 0) {
            throw new ServiceException(400, "积分不足");
        }
        int added = userMapper.addPointsById(toUserId, amount);
        if (added <= 0) {
            throw new ServiceException(404, "收款用户不存在");
        }
        insertLog(fromUserId, -amount, outReason);
        insertLog(toUserId, amount, inReason);
        activityEventPublisher.publish(ActivityType.POINTS_CHANGED, fromUserId, Map.of("points", getTotalPoints(fromUserId)));
        activityEventPublisher.publish(ActivityType.POINTS_CHANGED, toUserId, Map.of("points", getTotalPoints(toUserId)));
    }

    private void insertLog(Long userId, int delta, String reason) {
        PointsLog log = new PointsLog();
        log.setUserId(userId);
        log.setPointsChange(delta);
        log.setReason(reason);
        log.setCreateTime(LocalDateTime.now());
        pointsLogMapper.insert(log);
    }

    @Override
    public PointsVo getPoints(Long userId) {
        int total = getTotalPoints(userId);
        List<PointsLog> logs;
        try {
            logs = pointsLogMapper.selectList(new LambdaQueryWrapper<PointsLog>()
                    .eq(PointsLog::getUserId, userId)
                    .orderByDesc(PointsLog::getCreateTime)
                    .last("LIMIT 20"));
        } catch (Exception e) {
            logs = List.of();
        }
        List<PointsVo.PointsLogVo> recent = logs.stream()
                .map(l -> new PointsVo.PointsLogVo(l.getPointsChange(), l.getReason(), l.getCreateTime()))
                .collect(Collectors.toList());
        return new PointsVo(total, recent);
    }

    @Override
    public int getTotalPoints(Long userId) {
        try {
            Integer points = userMapper.selectPointsById(userId);
            return points != null ? points : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
