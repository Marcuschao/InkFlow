package com.blog.common.feign;

import com.blog.common.dto.SeoGenerateRequest;
import com.blog.common.dto.SeoGenerateResult;
import com.blog.common.support.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "blog-ai", contextId = "aiFeignClient")
public interface AiFeignClient {

    @PostMapping("/internal/ai/articles/{id}/seo")
    Result<SeoGenerateResult> generateSeo(@PathVariable("id") Long id, @RequestBody SeoGenerateRequest request);
}
