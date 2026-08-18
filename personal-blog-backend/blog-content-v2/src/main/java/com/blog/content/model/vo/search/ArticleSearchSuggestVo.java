package com.blog.content.model.vo.search;

import lombok.Data;

@Data
public class ArticleSearchSuggestVo {
    private Long id;
    private String title;
    private String highlightTitle;
}
