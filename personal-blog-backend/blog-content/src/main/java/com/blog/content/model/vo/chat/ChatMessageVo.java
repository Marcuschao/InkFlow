package com.blog.content.model.vo.chat;

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
    private Boolean recalled;
    private String clientMsgId;
    private LocalDateTime createTime;
}
