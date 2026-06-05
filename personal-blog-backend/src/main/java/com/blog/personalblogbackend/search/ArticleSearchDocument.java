package com.blog.personalblogbackend.search;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleSearchDocument {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String cover;
    private Long categoryId;
    private Long authorId;
    private Integer status;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
