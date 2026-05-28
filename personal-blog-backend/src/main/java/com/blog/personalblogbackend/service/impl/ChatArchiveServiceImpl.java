package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.config.websocket.ChatProperties;
import com.blog.personalblogbackend.mapper.ChatMessageMapper;
import com.blog.personalblogbackend.model.entity.ChatMessage;
import com.blog.personalblogbackend.model.vo.chat.ChatMessageVo;
import com.blog.personalblogbackend.service.ChatArchiveService;
import com.blog.personalblogbackend.service.ChatArchiveStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatArchiveServiceImpl implements ChatArchiveService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ChatMessageMapper chatMessageMapper;
    private final ChatProperties chatProperties;
    private final ObjectProvider<ChatArchiveStorageServiceImpl> archiveStorageProvider;

    @Override
    @Transactional
    public void archiveExpiredMessages() {
        ChatArchiveStorageServiceImpl storage = archiveStorageProvider.getIfAvailable();
        if (storage == null) {
            log.debug("MinIO 未启用，跳过聊天归档");
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now(ZONE).minusDays(Math.max(1, chatProperties.getArchiveHotDays()));
        int batchSize = Math.max(100, chatProperties.getArchiveBatchSize());
        int totalArchived = 0;
        while (true) {
            List<ChatMessage> rows = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                    .lt(ChatMessage::getCreateTime, cutoff)
                    .orderByAsc(ChatMessage::getCreateTime)
                    .last("LIMIT " + batchSize));
            if (rows.isEmpty()) {
                break;
            }
            Map<LocalDate, List<ChatMessageVo>> grouped = new LinkedHashMap<>();
            for (ChatMessage row : rows) {
                LocalDate day = row.getCreateTime().toLocalDate();
                grouped.computeIfAbsent(day, k -> new ArrayList<>()).add(toVo(row));
            }
            for (Map.Entry<LocalDate, List<ChatMessageVo>> entry : grouped.entrySet()) {
                int shard = storage.nextShardIndex(entry.getKey());
                storage.uploadDayShard(entry.getKey(), shard, entry.getValue());
            }
            List<Long> ids = rows.stream().map(ChatMessage::getId).collect(Collectors.toList());
            chatMessageMapper.deleteBatchIds(ids);
            totalArchived += ids.size();
            if (rows.size() < batchSize) {
                break;
            }
        }
        if (totalArchived > 0) {
            log.info("聊天消息归档完成 count={}", totalArchived);
        }
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
