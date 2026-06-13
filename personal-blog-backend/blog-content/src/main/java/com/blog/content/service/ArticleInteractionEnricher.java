package com.blog.content.service;

import com.blog.content.model.vo.ArticleVO;

public interface ArticleInteractionEnricher {

    void enrich(ArticleVO vo, Long viewerId);

    ArticleVO toSummaryVo(com.blog.content.model.entity.Article article);
}
