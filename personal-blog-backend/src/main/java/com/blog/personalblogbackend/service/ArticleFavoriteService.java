package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.common.support.PageResult;
import com.blog.personalblogbackend.model.vo.ArticleVO;
import com.blog.personalblogbackend.model.vo.interaction.FavoriteStatusVo;

public interface ArticleFavoriteService {

    FavoriteStatusVo toggle(Long userId, Long articleId, String idempotencyKey);

    FavoriteStatusVo status(Long userId, Long articleId);

    PageResult<ArticleVO> listMine(Long userId, int page, int size);

    boolean isFavorited(Long userId, Long articleId);
}
