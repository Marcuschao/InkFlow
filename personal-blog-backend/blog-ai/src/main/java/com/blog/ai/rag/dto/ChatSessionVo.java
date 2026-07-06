package com.blog.ai.rag.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionVo {
    private Long id;
    private Long userId;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
