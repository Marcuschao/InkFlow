package com.blog.common.feign;

import com.blog.common.dto.KnowledgeGraphDto;
import com.blog.common.dto.TagNodeDetailDto;
import com.blog.common.support.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "blog-content", contextId = "knowledgeFeignClient")
public interface KnowledgeFeignClient {

    @GetMapping("/internal/knowledge/graph")
    Result<KnowledgeGraphDto> getGraph();

    @GetMapping("/internal/knowledge/node")
    Result<TagNodeDetailDto> getNode(@RequestParam("tagId") Long tagId);
}
