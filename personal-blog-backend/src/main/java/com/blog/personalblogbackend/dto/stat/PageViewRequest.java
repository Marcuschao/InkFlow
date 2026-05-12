package com.blog.personalblogbackend.dto.stat;

import lombok.Data;

@Data
public class PageViewRequest {
    private String page;
    private Long articleId;
    private String visitorId;
}
