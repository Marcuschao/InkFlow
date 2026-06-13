package com.blog.content.controller;

import com.blog.content.common.support.PageResult;
import com.blog.content.common.support.Result;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.model.vo.ArticleVO;
import com.blog.content.model.vo.interaction.FollotatusVo;
import com.blog.content.model.vo.interaction.FollowToggleVo;
import com.blog.content.model.vo.interaction.UserBriefVo;
import com.blog.content.service.ArticleFavoriteService;
import com.blog.content.service.UserFeedService;
import com.blog.content.service.UserFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserSocialController {

    @Autowired
    private UserFollowService userFollowService;
    @Autowired
    private ArticleFavoriteService articleFavoriteService;
    @Autowired
    private UserFeedService userFeedService;
    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping("/feed")
    public Result<PageResult<ArticleVO>> feed(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(userFeedService.feed(currentUserService.requireUserId(), page, size));
    }

    @GetMapping("/me/favorites")
    public Result<PageResult<ArticleVO>> myFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(articleFavoriteService.listMine(currentUserService.requireUserId(), page, size));
    }

    @PostMapping("/{id}/follow")
    public Result<FollowToggleVo> toggleFollow(@PathVariable Long id) {
        return Result.success(userFollowService.toggle(currentUserService.requireUserId(), id));
    }

    @GetMapping("/{id}/follow/status")
    public Result<FollotatusVo> followStatus(@PathVariable Long id) {
        return Result.success(userFollowService.status(currentUserService.optionalUserId(), id));
    }

    @GetMapping("/{id}/followers")
    public Result<PageResult<UserBriefVo>> followers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(userFollowService.followers(id, currentUserService.optionalUserId(), page, size));
    }

    @GetMapping("/{id}/following")
    public Result<PageResult<UserBriefVo>> following(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(userFollowService.following(id, currentUserService.optionalUserId(), page, size));
    }
}
