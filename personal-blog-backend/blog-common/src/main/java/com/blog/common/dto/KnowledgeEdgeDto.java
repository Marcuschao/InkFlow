package com.blog.common.dto;

import lombok.Data;

@Data
public class KnowledgeEdgeDto {
    private String source;
    private String target;
    private Double weight;
    private String type;
}
