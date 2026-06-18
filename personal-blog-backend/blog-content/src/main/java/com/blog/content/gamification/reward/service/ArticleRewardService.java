package com.blog.content.gamification.reward.service;

import com.blog.content.gamification.reward.model.vo.ArticleRewardVo;

import java.util.List;

public interface ArticleRewardService {
    ArticleRewardVo reward(Long articleId, Long fromUserId, int points);

    List<ArticleRewardVo> listArticleRewards(Long articleId);

    List<ArticleRewardVo> listUserHistory(Long userId);
}
