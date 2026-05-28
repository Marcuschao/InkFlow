package com.blog.personalblogbackend.model.dto.chat;

import com.blog.personalblogbackend.model.vo.chat.ChatMessageVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResult {
    private List<ChatMessageVo> messages;
    private boolean hasMore;
}
