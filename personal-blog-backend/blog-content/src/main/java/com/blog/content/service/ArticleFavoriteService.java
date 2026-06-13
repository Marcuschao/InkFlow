package com.blog.content.service;

import com.blog.content.common.support.PageResult;
import com.blog.content.model.vo.ArticleVO;
import com.blog.content.model.vo.interaction.FavoriteStatusVo;

public interface ArticleFavoriteService {

    FavoriteStatusVo toggle(Long userId, Long articleId, String idempotencyKey);

    FavoriteStatusVo status(Long userId, Long articleId);

    PageResult<ArticleVO> listMine(Long userId, int page, int size);

    boolean isFavorited(Long userId, Long articleId);
}
