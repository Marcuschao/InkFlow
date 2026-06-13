package com.blog.content.model.dto.agent;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecommendArticleDto {
    private Long id;
    private String title;
    private String summary;
    private String cover;
    private LocalDateTime createTime;
    private String reason;
}
