package com.blog.content.model.dto.chat;

import com.blog.content.model.vo.chat.ChatMessageVo;
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
