package com.blog.ai.model.dto.agent;

import lombok.Data;

@Data
public class OutlineRequest {
    private String title;
    private String summary;
    private String topic;
}
