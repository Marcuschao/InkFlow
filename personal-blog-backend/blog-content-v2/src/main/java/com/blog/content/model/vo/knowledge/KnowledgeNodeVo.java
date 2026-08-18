package com.blog.content.model.vo.knowledge;

import lombok.Data;

@Data
public class KnowledgeNodeVo {
    private String id;
    private String type;
    private String label;
    private Long refId;
    private Integer articleCount;
    private Double weight;
}
