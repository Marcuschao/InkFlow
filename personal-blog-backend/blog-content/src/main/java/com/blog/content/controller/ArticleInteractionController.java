package com.blog.content.controller;

import com.blog.content.common.support.Result;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.model.vo.interaction.FavoriteStatusVo;
import com.blog.content.model.vo.interaction.LikeCountVo;
import com.blog.content.model.vo.interaction.LikeStatusVo;
import com.blog.content.service.ArticleFavoriteService;
import com.blog.content.service.ArticleLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class ArticleInteractionController {

    @Autowired
    private ArticleLikeService articleLikeService;
    @Autowired
    private ArticleFavoriteService articleFavoriteService;
    @Autowired
    private CurrentUserService currentUserService;

    @PostMapping("/{id}/like")
    public Result<LikeStatusVo> toggleLike(@PathVariable Long id,
                                           @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return Result.success(articleLikeService.toggle(currentUserService.requireUserId(), id, idempotencyKey));
    }

    @GetMapping("/{id}/like/status")
    public Result<LikeStatusVo> likeStatus(@PathVariable Long id) {
        return Result.success(articleLikeService.status(currentUserService.requireUserId(), id));
    }

    @GetMapping("/{id}/likes/count")
    public Result<LikeCountVo> likeCount(@PathVariable Long id) {
        return Result.success(articleLikeService.publicCount(id));
    }

    @PostMapping("/{id}/favorite")
    public Result<FavoriteStatusVo> toggleFavorite(@PathVariable Long id,
                                                 @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return Result.success(articleFavoriteService.toggle(currentUserService.requireUserId(), id, idempotencyKey));
    }

    @GetMapping("/{id}/favorite/status")
    public Result<FavoriteStatusVo> favoriteStatus(@PathVariable Long id) {
        return Result.success(articleFavoriteService.status(currentUserService.requireUserId(), id));
    }
}
