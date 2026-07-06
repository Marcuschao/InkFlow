package com.blog.ai.rag.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RagDebugVo {
    private String query;
    private List<SourceChunkDto> keywordHits = new ArrayList<>();
    private List<SourceChunkDto> vectorHits = new ArrayList<>();
    private List<SourceChunkDto> fused = new ArrayList<>();
    private List<SourceChunkDto> reranked = new ArrayList<>();
    private String answer;
    private List<SourceChunkDto> sources = new ArrayList<>();
}
