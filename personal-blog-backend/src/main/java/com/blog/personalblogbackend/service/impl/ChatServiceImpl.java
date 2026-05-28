package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.config.websocket.ChatProperties;
import com.blog.personalblogbackend.mapper.ChatMessageMapper;
import com.blog.personalblogbackend.model.dto.chat.ChatHistoryResult;
import com.blog.personalblogbackend.model.dto.chat.ChatSendRequest;
import com.blog.personalblogbackend.model.entity.ChatMessage;
import com.blog.personalblogbackend.model.vo.chat.ChatMessageVo;
import com.blog.personalblogbackend.service.ChatArchiveStorageService;
import com.blog.personalblogbackend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ChatMessageMapper chatMessageMapper;
    private final ChatProperties chatProperties;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectProvider<ChatArchiveStorageService> archiveStorageProvider;

    @Override
    public ChatHistoryResult loadHistory(Long cursor, Long afterId, Integer limit) {
        int pageSize = normalizeLimit(limit);
        if (afterId != null) {
            return loadAfterId(afterId, pageSize);
        }
        if (cursor == null) {
            return loadRecent(pageSize);
        }
        return loadBeforeCursor(cursor, pageSize);
    }

    @Override
    public ChatMessageVo send(Long userId, String username, String avatar, boolean admin, ChatSendRequest request) {
        if (userId == null || !StringUtils.hasText(username)) {
            throw new ServiceException(401, "未登录");
        }
        if (request == null || !StringUtils.hasText(request.getContent())) {
            throw new ServiceException(400, "消息内容不能为空");
        }
        String content = request.getContent().trim();
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ServiceException(400, "消息内容过长");
        }
        ChatMessage message = new ChatMessage();
        message.setUserId(userId);
        message.setUsername(username);
        message.setAvatar(avatar);
        message.setContent(content);
        message.setIsAdmin(admin ? 1 : 0);
        message.setCreateTime(LocalDateTime.now(ZONE));
        chatMessageMapper.insert(message);
        ChatMessageVo vo = toVo(message);
        messagingTemplate.convertAndSend("/topic/chat", vo);
        return vo;
    }

    private ChatHistoryResult loadRecent(int pageSize) {
        List<ChatMessage> rows = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + (pageSize + 1)));
        boolean hasMore = rows.size() > pageSize;
        if (hasMore) {
            rows = new ArrayList<>(rows.subList(0, pageSize));
        }
        Collections.reverse(rows);
        return new ChatHistoryResult(rows.stream().map(this::toVo).collect(Collectors.toList()), hasMore);
    }

    private ChatHistoryResult loadAfterId(Long afterId, int pageSize) {
        List<ChatMessage> rows = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .gt(ChatMessage::getId, afterId)
                .orderByAsc(ChatMessage::getId)
                .last("LIMIT " + (pageSize + 1)));
        boolean hasMore = rows.size() > pageSize;
        if (hasMore) {
            rows = new ArrayList<>(rows.subList(0, pageSize));
        }
        return new ChatHistoryResult(rows.stream().map(this::toVo).collect(Collectors.toList()), hasMore);
    }

    private ChatHistoryResult loadBeforeCursor(Long cursor, int pageSize) {
        List<ChatMessageVo> result = new ArrayList<>();
        List<ChatMessage> mysqlRows = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .lt(ChatMessage::getId, cursor)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + pageSize));
        for (ChatMessage row : mysqlRows) {
            result.add(toVo(row));
        }

        ChatArchiveStorageService archiveStorage = archiveStorageProvider.getIfAvailable();
        if (result.size() < pageSize && archiveStorage != null) {
            LocalDateTime anchorTime = resolveAnchorTime(cursor, mysqlRows);
            List<ChatMessageVo> archived = archiveStorage.readMessagesBefore(cursor, anchorTime, pageSize - result.size());
            mergeUnique(result, archived);
        }

        result.sort(Comparator.comparing(ChatMessageVo::getId));
        boolean hasMore = result.size() >= pageSize;
        if (result.size() > pageSize) {
            result = new ArrayList<>(result.subList(result.size() - pageSize, result.size()));
            hasMore = true;
        }
        return new ChatHistoryResult(result, hasMore);
    }

    private LocalDateTime resolveAnchorTime(Long cursor, List<ChatMessage> mysqlRows) {
        ChatMessage cursorRow = chatMessageMapper.selectById(cursor);
        if (cursorRow != null && cursorRow.getCreateTime() != null) {
            return cursorRow.getCreateTime();
        }
        if (!mysqlRows.isEmpty()) {
            ChatMessage oldest = mysqlRows.get(mysqlRows.size() - 1);
            if (oldest.getCreateTime() != null) {
                return oldest.getCreateTime();
            }
        }
        return LocalDateTime.now(ZONE).minusDays(Math.max(1, chatProperties.getArchiveHotDays()));
    }

    private static void mergeUnique(List<ChatMessageVo> target, List<ChatMessageVo> extra) {
        for (ChatMessageVo vo : extra) {
            if (vo.getId() == null) {
                continue;
            }
            boolean exists = target.stream().anyMatch(item -> vo.getId().equals(item.getId()));
            if (!exists) {
                target.add(vo);
            }
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return Math.max(1, chatProperties.getHistoryLimit());
        }
        return Math.min(limit, 100);
    }

    private ChatMessageVo toVo(ChatMessage message) {
        ChatMessageVo vo = new ChatMessageVo();
        vo.setId(message.getId());
        vo.setUserId(message.getUserId());
        vo.setUsername(message.getUsername());
        vo.setAvatar(message.getAvatar());
        vo.setContent(message.getContent());
        vo.setAdmin(message.getIsAdmin() != null && message.getIsAdmin() == 1);
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
