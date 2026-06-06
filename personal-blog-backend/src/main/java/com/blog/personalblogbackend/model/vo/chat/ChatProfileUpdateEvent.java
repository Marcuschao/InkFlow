package com.blog.personalblogbackend.model.vo.chat;

import lombok.Data;

@Data
public class ChatProfileUpdateEvent {
    private Long userId;
    private String username;
    private String avatar;
}
