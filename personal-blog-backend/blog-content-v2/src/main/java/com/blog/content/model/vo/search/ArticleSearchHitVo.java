package com.blog.content.model.vo.search;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleSearchHitVo {
    private Long id;
    private String title;
    private String summary;
    private String cover;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String highlightTitle;
    private String highlightSummary;
}
