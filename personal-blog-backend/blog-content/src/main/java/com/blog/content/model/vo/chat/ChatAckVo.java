package com.blog.content.model.vo.chat;

import lombok.Data;

@Data
public class ChatAckVo {
    private String clientMsgId;
    private Long messageId;
    private String status;
}
