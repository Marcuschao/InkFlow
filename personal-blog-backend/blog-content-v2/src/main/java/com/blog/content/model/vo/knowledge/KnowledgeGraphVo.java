package com.blog.content.model.vo.knowledge;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeGraphVo {
    private List<KnowledgeNodeVo> nodes;
    private List<KnowledgeEdgeVo> edges;
}
