package com.blog.ai.service;

import com.blog.common.dto.SeoGenerateRequest;
import com.blog.common.dto.SeoGenerateResult;

public interface ArticleSeoService {
    SeoGenerateResult generateSeo(Long articleId, SeoGenerateRequest request);
    void generateSeoByAi(Long articleId);
}
