package com.blog.ai.rag.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RagAnswerVo {
    private String answer;
    private List<SourceChunkDto> sources = new ArrayList<>();
}
