package com.blog.content.knowledge.service.impl;

import com.blog.common.dto.AutoTagItemDto;
import com.blog.common.dto.AutoTagRequest;
import com.blog.common.feign.AiAgentFeignClient;
import com.blog.common.support.Result;
import com.blog.content.knowledge.service.KnowledgeAutoTagService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeAutoTagServiceImpl implements KnowledgeAutoTagService {

    private final AiAgentFeignClient aiAgentFeignClient;

    public KnowledgeAutoTagServiceImpl(AiAgentFeignClient aiAgentFeignClient) {
        this.aiAgentFeignClient = aiAgentFeignClient;
    }

    @Override
    public List<String> generateTags(String title, String content) {
        AutoTagRequest req = new AutoTagRequest();
        req.setTitle(title);
        req.setContent(content);
        try {
            Result<List<AutoTagItemDto>> result = aiAgentFeignClient.autoTag(req);
            if (result == null || result.getCode() == null || result.getCode() != 200
                    || CollectionUtils.isEmpty(result.getData())) {
                return Collections.emptyList();
            }
            return result.getData().stream()
                    .map(AutoTagItemDto::getName)
                    .filter(n -> n != null && !n.isBlank())
                    .collect(Collectors.toList());
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
