package com.blog.content.gamification.points.controller;

import com.blog.content.common.support.Result;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.gamification.points.model.vo.PointsVo;
import com.blog.content.gamification.points.service.PointsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class PointsController {

    private final PointsService pointsService;
    private final CurrentUserService currentUserService;

    public PointsController(PointsService pointsService, CurrentUserService currentUserService) {
        this.pointsService = pointsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/points")
    public Result<PointsVo> points() {
        return Result.success(pointsService.getPoints(currentUserService.requireUserId()));
    }
}
