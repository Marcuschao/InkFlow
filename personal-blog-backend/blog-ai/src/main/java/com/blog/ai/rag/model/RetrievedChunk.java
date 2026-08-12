package com.blog.ai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunk {
    public RetrievedChunk(String chunkId, Long docId, String docTitle, Integer ordinal, String text, double score, double rerankScore) {
        this.chunkId = chunkId; this.docId = docId; this.docTitle = docTitle; this.ordinal = ordinal;
        this.text = text; this.score = score; this.rerankScore = rerankScore;
    }
    private String chunkId;
    private Long docId;
    private String docTitle;
    private Integer ordinal;
    private String text;
    private double score;
    private double rerankScore;
    private String tenantId;
    private String workspaceId;
    private Long ownerId;
    private String visibility;
    private Long documentVersion;
}
