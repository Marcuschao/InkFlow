package com.blog.content.profile.controller;

import com.blog.content.common.exception.ServiceException;
import com.blog.content.common.support.Result;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.profile.model.vo.SocialCardVo;
import com.blog.content.profile.model.vo.TimelineItemVo;
import com.blog.content.profile.model.vo.VisitorVo;
import com.blog.content.profile.service.SocialCardService;
import com.blog.content.profile.service.TimelineService;
import com.blog.content.profile.service.VisitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final VisitorService visitorService;
    private final TimelineService timelineService;
    private final SocialCardService socialCardService;
    private final CurrentUserService currentUserService;

    public ProfileController(VisitorService visitorService,
                               TimelineService timelineService,
                               SocialCardService socialCardService,
                               CurrentUserService currentUserService) {
        this.visitorService = visitorService;
        this.timelineService = timelineService;
        this.socialCardService = socialCardService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/profile/visit/{ownerId}")
    public Result<Void> recordVisit(@PathVariable Long ownerId) {
        Long visitorId = currentUserService.requireUserId();
        visitorService.recordVisit(ownerId, visitorId);
        return Result.success();
    }

    @GetMapping("/profile/visitors")
    public Result<List<VisitorVo>> visitors() {
        Long userId = currentUserService.requireUserId();
        return Result.success(visitorService.recentVisitors(userId, 20));
    }

    @GetMapping("/user/timeline")
    public Result<List<TimelineItemVo>> timeline(
            @RequestParam(defaultValue = "interaction") String type,
            @RequestParam(required = false) Long userId) {
        if (!"interaction".equals(type)) {
            throw new ServiceException(400, "不支持的时间线类型");
        }
        Long targetId = userId != null ? userId : currentUserService.optionalUserId();
        if (targetId == null) {
            throw new ServiceException(400, "缺少 userId");
        }
        return Result.success(timelineService.interactionTimeline(targetId, 20));
    }

    @GetMapping("/user/{id}/social-card")
    public Result<SocialCardVo> socialCard(@PathVariable Long id) {
        return Result.success(socialCardService.getSocialCard(id, currentUserService.optionalUserId()));
    }
}
