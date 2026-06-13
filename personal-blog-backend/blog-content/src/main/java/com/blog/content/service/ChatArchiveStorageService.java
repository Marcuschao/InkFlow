package com.blog.content.service;

import com.blog.content.model.vo.chat.ChatMessageVo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ChatArchiveStorageService {

    void uploadDayShard(LocalDate day, int shardIndex, List<ChatMessageVo> messages);

    List<ChatMessageVo> readMessagesBefore(Long cursorId, LocalDateTime anchorTime, int limit);
}
