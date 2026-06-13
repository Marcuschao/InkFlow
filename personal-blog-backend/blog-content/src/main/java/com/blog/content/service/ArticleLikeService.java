package com.blog.content.service;

import com.blog.content.model.vo.interaction.LikeCountVo;
import com.blog.content.model.vo.interaction.LikeStatusVo;

public interface ArticleLikeService {

    LikeStatusVo toggle(Long userId, Long articleId, String idempotencyKey);

    LikeStatusVo status(Long userId, Long articleId);

    LikeCountVo publicCount(Long articleId);

    boolean isLiked(Long userId, Long articleId);
}
