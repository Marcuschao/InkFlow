package com.blog.ai.rag.generate;

import com.blog.ai.gateway.AiTaskType;
import com.blog.ai.llm.AiService;
import com.blog.ai.gateway.model.GatewayResult;
import com.blog.ai.rag.dto.RagAnswerVo;
import com.blog.ai.rag.dto.SourceChunkDto;
import com.blog.ai.rag.model.RetrievedChunk;
import com.blog.ai.rag.retrieve.HybridRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class RagGenerationService {

    private final HybridRetrievalService hybridRetrievalService;
    private final AiService aiService;

    public RagAnswerVo answer(String query, String historySummary, String recentHistory) {
        HybridRetrievalService.RetrievalResult result = hybridRetrievalService.retrieve(query);
        List<RetrievedChunk> context = result.reranked;
        if (context == null || context.isEmpty()) {
            context = result.fused;
        }
        if (context == null || context.isEmpty()) {
            context = result.keywordHits;
        }
        List<SourceChunkDto> sources = toSources(context);
        String sys = "你是 InkFlow 平台知识库问答助手。请基于下面「参考资料」回答用户问题。"
                + "在答案中引用对应资料时使用 [1]、[2] 等标记，编号对应参考资料的序号。"
                + "若参考资料中有与问题相关的内容，请直接作答；仅当参考资料完全为空时才回复：抱歉，请询问与本站相关的内容，例如：如何开始学习微服务？\n。";
        StringBuilder ctx = new StringBuilder();
        for (int i = 0; i < context.size(); i++) {
            RetrievedChunk c = context.get(i);
            ctx.append("[").append(i + 1).append("] 来源文档：《").append(nullToEmpty(c.getDocTitle()))
                    .append("》 分块序号:").append(c.getOrdinal() != null ? c.getOrdinal() : 0)
                    .append("\n内容：").append(truncate(nullToEmpty(c.getText()), 800)).append("\n\n");
        }
        String user = "参考资料：\n" + ctx + "\n用户问题：" + query;
        if (StringUtils.hasText(recentHistory)) {
            user = "近期对话：\n" + recentHistory + "\n\n" + user;
        }
        if (StringUtils.hasText(historySummary)) {
            user = "更早对话摘要：" + historySummary + "\n\n" + user;
        }
        GatewayResult gatewayResult = aiService.chatResult(AiTaskType.RAG, sys, user);
        String answer = gatewayResult.getContent();
        RagAnswerVo vo = new RagAnswerVo();
        vo.setAnswer(answer);
        vo.setSources(sources);
        vo.setTotalTokens(gatewayResult.getTotalTokens());
        return vo;
    }

    public List<SourceChunkDto> toSources(List<RetrievedChunk> chunks) {
        List<SourceChunkDto> out = new ArrayList<>();
        if (chunks == null) return out;
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk c = chunks.get(i);
            SourceChunkDto s = new SourceChunkDto();
            s.setChunkId(c.getChunkId());
            s.setDocId(c.getDocId());
            s.setDocTitle(c.getDocTitle());
            s.setOrdinal(c.getOrdinal());
            s.setSnippet(truncate(nullToEmpty(c.getText()), 200));
            s.setScore(c.getScore());
            s.setRerankScore(c.getRerankScore());
            s.setLink("/api/admin/knowledge/documents/" + c.getDocId());
            out.add(s);
        }
        return out;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
