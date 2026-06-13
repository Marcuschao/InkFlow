package com.blog.content.service;

public interface ChatRecallService {
    void recall(Long messageId, Long operatorId, boolean admin);
}
