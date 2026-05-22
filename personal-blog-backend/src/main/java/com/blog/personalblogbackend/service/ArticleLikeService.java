package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.model.vo.interaction.LikeCountVo;
import com.blog.personalblogbackend.model.vo.interaction.LikeStatusVo;

public interface ArticleLikeService {

    LikeStatusVo toggle(Long userId, Long articleId);

    LikeStatusVo status(Long userId, Long articleId);

    LikeCountVo publicCount(Long articleId);

    boolean isLiked(Long userId, Long articleId);
}
