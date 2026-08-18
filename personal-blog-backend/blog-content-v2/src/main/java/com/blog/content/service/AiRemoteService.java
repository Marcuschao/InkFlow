package com.blog.content.service;

public interface AiRemoteService {
    void generateSeoByAi(Long articleId, String title, String summary, String content);
}
