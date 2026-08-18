package com.blog.content.model.dto.chat;

import lombok.Data;

@Data
public class ChatSendRequest {
    private String content;
    private String clientMsgId;
}
