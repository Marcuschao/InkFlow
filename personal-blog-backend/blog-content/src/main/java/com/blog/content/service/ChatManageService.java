package com.blog.content.service;

import com.blog.content.common.support.PageResult;
import com.blog.content.model.entity.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatManageService {
    PageResult<ChatMessage> page(long page, long size, String username, String keyword,
                                 LocalDateTime start, LocalDateTime end);

    void deleteOne(Long id);

    void deleteBatch(List<Long> ids);
}
