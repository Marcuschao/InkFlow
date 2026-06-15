package com.blog.common.dto;

import lombok.Data;

@Data
public class KnowledgeNodeDto {
    private String id;
    private String type;
    private String label;
    private Long refId;
    private Integer articleCount;
    private Double weight;
}
