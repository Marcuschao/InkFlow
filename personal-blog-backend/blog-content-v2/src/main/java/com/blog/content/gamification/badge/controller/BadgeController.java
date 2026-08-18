package com.blog.content.gamification.badge.controller;

import com.blog.content.common.support.Result;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.gamification.badge.model.vo.BadgeListVo;
import com.blog.content.gamification.badge.model.vo.BadgeVo;
import com.blog.content.gamification.badge.service.BadgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    private final BadgeService badgeService;
    private final CurrentUserService currentUserService;

    public BadgeController(BadgeService badgeService, CurrentUserService currentUserService) {
        this.badgeService = badgeService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Result<BadgeListVo> listAll() {
        return Result.success(badgeService.listAllWithEarned(currentUserService.optionalUserId()));
    }

    @GetMapping("/user/{userId}")
    public Result<List<BadgeVo>> userBadges(@PathVariable Long userId) {
        return Result.success(badgeService.listUserBadges(userId));
    }
}
