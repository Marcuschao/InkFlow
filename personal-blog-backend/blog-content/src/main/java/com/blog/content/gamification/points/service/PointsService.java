package com.blog.content.gamification.points.service;

import com.blog.content.gamification.points.model.vo.PointsVo;

public interface PointsService {

    void addPoints(Long userId, int delta, String reason);

    boolean deductPoints(Long userId, int amount, String reason);

    void transferPoints(Long fromUserId, Long toUserId, int amount, String outReason, String inReason);

    PointsVo getPoints(Long userId);

    int getTotalPoints(Long userId);
}
