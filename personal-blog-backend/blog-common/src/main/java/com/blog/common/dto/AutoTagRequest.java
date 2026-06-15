package com.blog.common.dto;

import lombok.Data;

@Data
public class AutoTagRequest {
    private Long articleId;
    private String title;
    private String content;
}
