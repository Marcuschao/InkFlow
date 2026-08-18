package com.blog.ai.rag.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class KnowledgeChunkDoc {
    private String chunkId;
    private Long docId;
    private String docTitle;
    private Integer ordinal;
    private String text;
    private List<Float> embedding;
    private Map<String, Object> metadata;
    /** 数据治理字段，写入索引后用于服务端权限过滤。 */
    private String tenantId;
    private String workspaceId;
    private Long ownerId;
    private String visibility = "PUBLIC";
    private Long documentVersion;
}
