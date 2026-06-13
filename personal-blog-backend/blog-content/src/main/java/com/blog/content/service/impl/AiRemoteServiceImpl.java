package com.blog.content.service.impl;

import com.blog.common.dto.SeoGenerateRequest;
import com.blog.common.dto.SeoGenerateResult;
import com.blog.common.feign.AiFeignClient;
import com.blog.common.support.Result;
import com.blog.content.common.exception.ServiceException;
import com.blog.content.service.AiRemoteService;
import org.springframework.stereotype.Service;

@Service
public class AiRemoteServiceImpl implements AiRemoteService {

    private final AiFeignClient aiFeignClient;

    public AiRemoteServiceImpl(AiFeignClient aiFeignClient) {
        this.aiFeignClient = aiFeignClient;
    }

    @Override
    public void generateSeoByAi(Long articleId, String title, String summary, String content) {
        SeoGenerateRequest req = new SeoGenerateRequest();
        req.setTitle(title);
        req.setSummary(summary);
        req.setContent(content);
        Result<SeoGenerateResult> result = aiFeignClient.generateSeo(articleId, req);
        if (result == null || result.getCode() == null || result.getCode() != 200) {
            throw new ServiceException(500, result != null ? result.getMessage() : "AI 服务调用失败");
        }
    }
}
