package com.blog.common.feign;

import com.blog.common.dto.AutoTagItemDto;
import com.blog.common.dto.AutoTagRequest;
import com.blog.common.dto.LearningPathRequest;
import com.blog.common.dto.LearningPathResult;
import com.blog.common.support.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "blog-ai", contextId = "aiAgentFeignClient")
public interface AiAgentFeignClient {

    @PostMapping("/internal/ai/auto-tag")
    Result<List<AutoTagItemDto>> autoTag(@RequestBody AutoTagRequest request);

    @PostMapping("/internal/ai/learning-path")
    Result<LearningPathResult> learningPath(@RequestBody LearningPathRequest request);

    @GetMapping("/internal/ai/weekly-insight")
    Result<String> weeklyInsight();
}
