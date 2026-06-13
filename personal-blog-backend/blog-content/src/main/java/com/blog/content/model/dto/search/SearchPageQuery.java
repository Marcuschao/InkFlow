package com.blog.content.model.dto.search;

import lombok.Data;

@Data
public class SearchPageQuery {
    private String keyword;
    private Integer page = 1;
    private Integer size = 10;
}
