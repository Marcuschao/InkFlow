package com.blog.ai.rag;

import com.blog.ai.model.dto.agent.ChatResponse;
import com.blog.ai.model.dto.agent.ChatSourceDto;
import com.blog.ai.model.entity.ChatMessage;
import com.blog.ai.rag.dto.RagAnswerVo;
import com.blog.ai.rag.dto.SourceChunkDto;
import com.blog.ai.rag.generate.RagGenerationService;
import com.blog.ai.rag.session.ChatPrincipal;
import com.blog.ai.rag.session.ChatPrincipalResolver;
import com.blog.ai.rag.session.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class RagChatOrchestrator {
    private final ChatSessionService chatSessionService;
    private final RagGenerationService ragGenerationService;
    private final ChatPrincipalResolver principalResolver;

    public ChatResponse chat(String question, Long sessionId) {
        return chat(question, sessionId, principalResolver.resolveCurrent());
    }

    public ChatResponse chat(String question, Long sessionId, ChatPrincipal principal) {
        var session = chatSessionService.ensureSession(sessionId, principal, question);
        String recentHistory = chatSessionService.formatRecentHistory(session.getId(), 12);
        String historySummary = chatSessionService.compressHistory(session.getId(), question);
        chatSessionService.saveMessage(session.getId(), "user", question, null);
        RagAnswerVo answer = ragGenerationService.answer(question, historySummary, recentHistory);
        ChatMessage assistant = chatSessionService.saveMessage(
                session.getId(), "assistant", answer.getAnswer(), answer.getSources());

        ChatResponse response = new ChatResponse();
        response.setAnswer(answer.getAnswer());
        response.setSources(toChatSources(answer.getSources()));
        response.setSessionId(session.getId());
        response.setMessageId(assistant.getId());
        response.setGrounded(answer.isGrounded());
        response.setConfidence(answer.getConfidence());
        response.setRefusalReason(answer.getRefusalReason());
        response.setDegraded(answer.isDegraded());
        return response;
    }

    private List<ChatSourceDto> toChatSources(List<SourceChunkDto> sources) {
        List<ChatSourceDto> result = new ArrayList<>();
        if (sources == null) return result;
        for (SourceChunkDto source : sources) {
            ChatSourceDto item = new ChatSourceDto();
            item.setId(source.getDocId());
            item.setTitle(source.getDocTitle());
            item.setChunkId(source.getChunkId());
            item.setOrdinal(source.getOrdinal());
            item.setSnippet(source.getSnippet());
            item.setScore(source.getScore());
            item.setLink(source.getLink());
            result.add(item);
        }
        return result;
    }
}
