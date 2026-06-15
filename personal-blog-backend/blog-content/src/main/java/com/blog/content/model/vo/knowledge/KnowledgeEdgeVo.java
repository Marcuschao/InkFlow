package com.blog.content.model.vo.knowledge;

import lombok.Data;

@Data
public class KnowledgeEdgeVo {
    private String source;
    private String target;
    private Double weight;
    private String type;
}
