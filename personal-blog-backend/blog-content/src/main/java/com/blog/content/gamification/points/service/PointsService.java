package com.blog.content.gamification.points.service;

import com.blog.content.gamification.points.model.vo.PointsVo;

public interface PointsService {

    void addPoints(Long userId, int delta, String reason);

    PointsVo getPoints(Long userId);

    int getTotalPoints(Long userId);
}
