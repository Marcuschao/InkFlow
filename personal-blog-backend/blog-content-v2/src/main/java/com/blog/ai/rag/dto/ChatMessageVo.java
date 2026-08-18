package com.blog.ai.rag.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVo {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private Object sources;
    private LocalDateTime createTime;
}
