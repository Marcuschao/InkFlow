package com.blog.personalblogbackend.model.vo.chat;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVo {
    private Long id;
    private Long userId;
    private String username;
    private String avatar;
    private String content;
    private Boolean admin;
    private LocalDateTime createTime;
}
