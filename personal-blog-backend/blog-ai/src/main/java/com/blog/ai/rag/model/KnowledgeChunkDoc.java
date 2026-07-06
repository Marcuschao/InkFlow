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
}
