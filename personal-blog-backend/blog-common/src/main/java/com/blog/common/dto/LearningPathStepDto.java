package com.blog.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class LearningPathStepDto {
    private String title;
    private String description;
    private List<Long> articleIds;
}
