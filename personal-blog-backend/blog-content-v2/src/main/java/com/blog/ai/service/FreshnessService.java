package com.blog.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.ai.model.dto.freshness.FreshnessAiDraftDto;
import com.blog.ai.model.dto.freshness.FreshnessSummaryDto;
import com.blog.ai.model.entity.Article;

public interface FreshnessService {

    FreshnessSummaryDto summary();

    IPage<Article> pagePublishedByFreshness(Page<Article> page, Integer freshnessStatus);

    FreshnessAiDraftDto aiRefreshDraft(Long articleId);

    void runFullScan();
}
