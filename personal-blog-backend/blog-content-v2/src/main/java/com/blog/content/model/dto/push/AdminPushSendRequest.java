package com.blog.content.model.dto.push;

import lombok.Data;

@Data
public class AdminPushSendRequest {
    private Long articleId;
    private String title;
    private String body;
    private String url;
}
