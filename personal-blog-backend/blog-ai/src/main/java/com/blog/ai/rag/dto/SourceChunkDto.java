package com.blog.ai.rag.dto;

import lombok.Data;

@Data
public class SourceChunkDto {
    private String chunkId;
    private Long docId;
    private String docTitle;
    private Integer ordinal;
    private String snippet;
    private Double score;
    private Double rerankScore;
    private String link;
}
