package com.blog.ai.controller;

import com.blog.ai.common.support.Result;
import com.blog.ai.config.properties.RagProperties;
import com.blog.ai.rag.dto.RagAnswerVo;
import com.blog.ai.rag.dto.RagDebugVo;
import com.blog.ai.rag.dto.SourceChunkDto;
import com.blog.ai.rag.generate.RagGenerationService;
import com.blog.ai.rag.retrieve.HybridRetrievalService;
import com.blog.ai.rag.search.KnowledgeChunkIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/rag")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class RagDebugController {

    private final HybridRetrievalService hybridRetrievalService;
    private final RagGenerationService ragGenerationService;
    private final KnowledgeChunkIndexService indexService;
    private final RagProperties properties;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> m = new HashMap<>();
        m.put("esChunkCount", indexService.countAll());
        m.put("chunksIndex", properties.getEs().getChunksIndex());
        m.put("vectorDims", properties.getEs().getVectorDims());
        return Result.success(m);
    }

    @PostMapping("/debug")
    public Result<RagDebugVo> debug(@RequestParam String query) {
        HybridRetrievalService.RetrievalResult result = hybridRetrievalService.retrieve(query);
        RagAnswerVo answer = ragGenerationService.answer(query, null, null);
        RagDebugVo vo = new RagDebugVo();
        vo.setQuery(query);
        vo.setKeywordHits(ragGenerationService.toSources(result.keywordHits));
        vo.setVectorHits(ragGenerationService.toSources(result.vectorHits));
        vo.setFused(ragGenerationService.toSources(result.fused));
        vo.setReranked(ragGenerationService.toSources(result.reranked));
        vo.setAnswer(answer.getAnswer());
        vo.setSources(answer.getSources());
        return Result.success(vo);
    }

    @GetMapping("/retrieve")
    public Result<List<SourceChunkDto>> retrieve(@RequestParam String query) {
        HybridRetrievalService.RetrievalResult result = hybridRetrievalService.retrieve(query);
        return Result.success(ragGenerationService.toSources(result.reranked));
    }
}
