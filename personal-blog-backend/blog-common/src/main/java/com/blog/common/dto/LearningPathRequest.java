package com.blog.common.dto;

import lombok.Data;

@Data
public class LearningPathRequest {
    private String goal;
    private Long userId;
    private Long startTagId;
    private Long endTagId;
}
