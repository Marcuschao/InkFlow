package com.blog.ai.rag;

import com.blog.ai.model.dto.agent.ChatResponse;
import com.blog.ai.model.dto.agent.ChatSourceDto;
import com.blog.common.security.JwtUtils;
import com.blog.ai.rag.dto.RagAnswerVo;
import com.blog.ai.rag.dto.SourceChunkDto;
import com.blog.ai.rag.generate.RagGenerationService;
import com.blog.ai.rag.session.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class RagChatOrchestrator {

    private final ChatSessionService chatSessionService;
    private final RagGenerationService ragGenerationService;
    private final JwtUtils jwtUtils;

    public ChatResponse chat(String question, Long sessionId, Long userId) {
        com.blog.ai.model.entity.ChatSession session =
                chatSessionService.ensureSession(sessionId, userId, question);
        String recentHistory = chatSessionService.formatRecentHistory(session.getId(), 12);
        String historySummary = chatSessionService.compressHistory(session.getId(), question);
        chatSessionService.saveMessage(session.getId(), "user", question, null);
        RagAnswerVo answerVo = ragGenerationService.answer(question, historySummary, recentHistory);
        List<ChatSourceDto> sources = toChatSources(answerVo.getSources());
        chatSessionService.saveMessage(session.getId(), "assistant", answerVo.getAnswer(), answerVo.getSources());
        ChatResponse resp = new ChatResponse();
        resp.setAnswer(answerVo.getAnswer());
        resp.setSources(sources);
        resp.setSessionId(session.getId());
        return resp;
    }

    private List<ChatSourceDto> toChatSources(List<SourceChunkDto> src) {
        List<ChatSourceDto> out = new ArrayList<>();
        if (src == null) return out;
        for (SourceChunkDto s : src) {
            ChatSourceDto d = new ChatSourceDto();
            d.setId(s.getDocId());
            d.setTitle(s.getDocTitle());
            d.setChunkId(s.getChunkId());
            d.setOrdinal(s.getOrdinal());
            d.setSnippet(s.getSnippet());
            d.setScore(s.getScore());
            d.setLink(s.getLink());
            out.add(d);
        }
        return out;
    }

    public Long currentUserId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes sra)) {
            return null;
        }
        String bearer = sra.getRequest().getHeader("Authorization");
        if (!StringUtils.hasText(bearer) || !bearer.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwtUtils.getUserIdFromToken(bearer.substring(7));
        } catch (Exception e) {
            return null;
        }
    }
}
