package com.blog.personalblogbackend.model.dto;

import lombok.Data;

@Data
public class ArticlePageQuery {
    private Long categoryId;
    private Long tagId;
    private String keyword;
    private Integer page = 1;
    private Integer size = 10;
    private Long lastId;
}
