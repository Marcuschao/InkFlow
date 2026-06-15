package com.blog.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class LearningPathResult {
    private Long id;
    private String name;
    private List<LearningPathStepDto> steps;
}
