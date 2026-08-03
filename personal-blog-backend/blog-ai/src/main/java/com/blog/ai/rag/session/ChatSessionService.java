package com.blog.ai.rag.session;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.config.properties.RagProperties;
import com.blog.ai.gateway.AiTaskType;
import com.blog.ai.llm.AiService;
import com.blog.ai.mapper.ChatMessageMapper;
import com.blog.ai.mapper.ChatSessionMapper;
import com.blog.ai.model.entity.ChatMessage;
import com.blog.ai.model.entity.ChatSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AiService aiService;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;

    public ChatSession ensureSession(Long sessionId, Long userId, String question) {
        if (sessionId != null) {
            ChatSession existing = chatSessionMapper.selectById(sessionId);
            if (existing != null) {
                if (userId != null && existing.getUserId() == null) {
                    ChatSession bind = new ChatSession();
                    bind.setId(existing.getId());
                    bind.setUserId(userId);
                    bind.setUpdateTime(LocalDateTime.now());
                    chatSessionMapper.updateById(bind);
                    existing.setUserId(userId);
                }
                return existing;
            }
        }
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(StringUtils.hasText(question) ? truncate(question, 80) : "新对话");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session;
    }

    public Page<ChatSession> pageSessions(Long userId, long page, long size) {
        QueryWrapper<ChatSession> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        qw.orderByDesc("update_time");
        return chatSessionMapper.selectPage(new Page<>(page, size), qw);
    }

    public void bindSessionsToUser(Collection<Long> sessionIds, Long userId) {
        if (userId == null || sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        for (Long sessionId : sessionIds) {
            if (sessionId == null) {
                continue;
            }
            ChatSession existing = chatSessionMapper.selectById(sessionId);
            if (existing != null && existing.getUserId() == null) {
                ChatSession bind = new ChatSession();
                bind.setId(sessionId);
                bind.setUserId(userId);
                bind.setUpdateTime(LocalDateTime.now());
                chatSessionMapper.updateById(bind);
            }
        }
    }

    public Page<ChatSession> pageSessionsByIds(List<Long> ids, long page, long size) {
        if (ids == null || ids.isEmpty()) {
            Page<ChatSession> empty = new Page<>(page, size, 0);
            empty.setRecords(Collections.emptyList());
            return empty;
        }
        QueryWrapper<ChatSession> qw = new QueryWrapper<>();
        qw.in("id", ids).orderByDesc("update_time");
        return chatSessionMapper.selectPage(new Page<>(page, size), qw);
    }

    public void assertSessionReadable(Long sessionId, Long userId, List<Long> guestSessionIds) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ServiceException(404, "会话不存在");
        }
        if (userId != null) {
            if (session.getUserId() != null && !userId.equals(session.getUserId())) {
                throw new ServiceException(403, "无权访问");
            }
            return;
        }
        if (session.getUserId() != null) {
            throw new ServiceException(403, "无权访问");
        }
        if (guestSessionIds == null || !guestSessionIds.contains(sessionId)) {
            throw new ServiceException(403, "无权访问");
        }
    }

    public String formatRecentHistory(Long sessionId, int maxMessages) {
        List<ChatMessage> messages = listMessages(sessionId);
        if (messages.isEmpty()) {
            return null;
        }
        int start = Math.max(0, messages.size() - maxMessages);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            sb.append(m.getRole()).append(": ").append(truncate(m.getContent(), 500)).append("\n");
        }
        return sb.toString();
    }

    public List<ChatMessage> listMessages(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ServiceException(404, "会话不存在");
        }
        return chatMessageMapper.selectList(new QueryWrapper<ChatMessage>()
                .eq("session_id", sessionId).orderByAsc("create_time"));
    }

    public void deleteSession(Long sessionId) {
        chatMessageMapper.delete(new QueryWrapper<ChatMessage>().eq("session_id", sessionId));
        chatSessionMapper.deleteById(sessionId);
    }

    public ChatMessage saveMessage(Long sessionId, String role, String content, Object sources) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreateTime(LocalDateTime.now());
        if (sources != null) {
            try {
                msg.setSourcesJson(objectMapper.writeValueAsString(sources));
            } catch (Exception e) {
                log.warn("[rag] serialize sources failed: {}", e.getMessage());
            }
        }
        chatMessageMapper.insert(msg);
        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(update);
        return msg;
    }

    public String compressHistory(Long sessionId, String currentQuestion) {
        List<ChatMessage> messages = listMessages(sessionId);
        if (messages.size() < ragProperties.getHistorySummaryThreshold()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, messages.size() - 10);
        for (int i = start; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            sb.append("role=").append(m.getRole()).append(": ").append(truncate(m.getContent(), 400)).append("\n");
        }
        String sys = "你是对话历史压缩助手。将以下多轮对话压缩为简短中文摘要，保留关键信息与用户意图，不要包含当前最新问题。";
        String user = "对话历史：\n" + sb;
        try {
            return aiService.chat(AiTaskType.RAG, sys, user);
        } catch (Exception e) {
            log.warn("[rag] compress history failed: {}", e.getMessage());
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
