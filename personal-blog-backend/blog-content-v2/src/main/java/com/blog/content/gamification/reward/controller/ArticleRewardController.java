package com.blog.content.gamification.reward.controller;

import com.blog.content.common.support.Result;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.gamification.reward.model.dto.ArticleRewardRequest;
import com.blog.content.gamification.reward.model.vo.ArticleRewardVo;
import com.blog.content.gamification.reward.service.ArticleRewardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ArticleRewardController {

    private final ArticleRewardService articleRewardService;
    private final CurrentUserService currentUserService;

    public ArticleRewardController(ArticleRewardService articleRewardService,
                                   CurrentUserService currentUserService) {
        this.articleRewardService = articleRewardService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/articles/{id}/reward")
    public Result<ArticleRewardVo> reward(@PathVariable Long id, @Valid @RequestBody ArticleRewardRequest request) {
        return Result.success(articleRewardService.reward(id, currentUserService.requireUserId(), request.getPoints()));
    }

    @GetMapping("/articles/{id}/rewards")
    public Result<List<ArticleRewardVo>> rewards(@PathVariable Long id) {
        return Result.success(articleRewardService.listArticleRewards(id));
    }

    @GetMapping("/user/reward-history")
    public Result<List<ArticleRewardVo>> history() {
        return Result.success(articleRewardService.listUserHistory(currentUserService.requireUserId()));
    }
}
