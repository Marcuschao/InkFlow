package com.blog.content.knowledge.service;

import java.util.List;

public interface KnowledgeAutoTagService {
    List<String> generateTags(String title, String content);
}
