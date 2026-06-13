package com.blog.ai.model.dto.agent;

import lombok.Data;

import java.util.List;

@Data
public class RecommendContextRequest {
    private Long articleId;
    private List<Long> recentArticleIds;
}
