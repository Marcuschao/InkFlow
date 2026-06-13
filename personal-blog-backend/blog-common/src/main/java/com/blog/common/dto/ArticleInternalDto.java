package com.blog.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleInternalDto {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String cover;
    private Long categoryId;
    private Long authorId;
    private Integer status;
    private Integer viewCount;
    private Integer freshnessStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String seoTitle;
    private String seoDescription;
}
