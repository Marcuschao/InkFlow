package com.blog.common.dto;

import lombok.Data;

@Data
public class SeoGenerateRequest {
    private String title;
    private String summary;
    private String content;
}
