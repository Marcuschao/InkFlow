package com.blog.content.knowledge.model;

import lombok.Data;

@Data
public class TagArticleCountDto {
    private Long tagId;
    private String name;
    private Integer articleCount;
}
