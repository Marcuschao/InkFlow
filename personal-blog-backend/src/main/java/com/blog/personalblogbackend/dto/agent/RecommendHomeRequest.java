package com.blog.personalblogbackend.dto.agent;

import lombok.Data;

import java.util.List;

@Data
public class RecommendHomeRequest {
    private List<Long> recentArticleIds;
}
