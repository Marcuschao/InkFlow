package com.blog.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeGraphDto {
    private List<KnowledgeNodeDto> nodes;
    private List<KnowledgeEdgeDto> edges;
}
