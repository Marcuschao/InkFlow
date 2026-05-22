package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.model.vo.ArticleVO;

public interface ArticleInteractionEnricher {

    void enrich(ArticleVO vo, Long viewerId);

    ArticleVO toSummaryVo(com.blog.personalblogbackend.model.entity.Article article);
}
