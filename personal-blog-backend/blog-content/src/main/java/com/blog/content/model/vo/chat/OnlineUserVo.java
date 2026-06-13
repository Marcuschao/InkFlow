package com.blog.content.model.vo.chat;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OnlineUserVo {
    private Long userId;
    private String username;
    private String avatar;
    private Boolean admin;
    private String ip;
    private String sessionId;
    private LocalDateTime onlineAt;
}
