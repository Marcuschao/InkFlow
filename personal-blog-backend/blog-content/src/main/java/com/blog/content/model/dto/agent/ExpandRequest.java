package com.blog.content.model.dto.agent;

import lombok.Data;

@Data
public class ExpandRequest {
    private String title;
    private String context;
    private String direction;
}
