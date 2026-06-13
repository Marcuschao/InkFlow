package com.blog.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class ArticleSearchRequest {
    private List<String> keywords;
    private Long excludeId;
    private List<Long> excludeIds;
    private Integer limit = 5;
}
