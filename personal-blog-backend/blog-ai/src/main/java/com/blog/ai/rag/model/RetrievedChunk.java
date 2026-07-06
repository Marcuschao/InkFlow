package com.blog.ai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunk {
    private String chunkId;
    private Long docId;
    private String docTitle;
    private Integer ordinal;
    private String text;
    private double score;
    private double rerankScore;
}
