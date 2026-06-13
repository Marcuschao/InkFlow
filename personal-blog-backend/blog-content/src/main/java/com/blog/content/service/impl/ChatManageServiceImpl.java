package com.blog.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.common.exception.ServiceException;
import com.blog.content.common.support.PageResult;
import com.blog.content.mapper.ChatMessageMapper;
import com.blog.content.mapper.UserMapper;
import com.blog.content.mapper.UserProfileMapper;
import com.blog.content.model.entity.ChatMessage;
import com.blog.content.model.entity.User;
import com.blog.content.model.entity.UserProfile;
import com.blog.content.service.ChatManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatManageServiceImpl implements ChatManageService {

    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    public PageResult<ChatMessage> page(long page, long size, String username, String keyword,
                                        LocalDateTime start, LocalDateTime end) {
        long pg = Math.max(page, 1);
        long sz = Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<ChatMessage> q = new LambdaQueryWrapper<ChatMessage>()
                .orderByDesc(ChatMessage::getId);
        if (StringUtils.hasText(username)) {
            Set<Long> matchedUserIds = findUserIdsByDisplayName(username.trim());
            if (matchedUserIds.isEmpty()) {
                return PageResult.build(List.of(), 0L, sz, pg);
            }
            q.in(ChatMessage::getUserId, matchedUserIds);
        }
        if (StringUtils.hasText(keyword)) {
            q.like(ChatMessage::getContent, keyword.trim());
        }
        if (start != null) {
            q.ge(ChatMessage::getCreateTime, start);
        }
        if (end != null) {
            q.le(ChatMessage::getCreateTime, end);
        }
        Page<ChatMessage> result = chatMessageMapper.selectPage(new Page<>(pg, sz), q);
        return PageResult.build(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public void deleteOne(Long id) {
        if (id == null) {
            throw new ServiceException(400, "ID 无效");
        }
        chatMessageMapper.deleteById(id);
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException(400, "请选择消息");
        }
        chatMessageMapper.deleteBatchIds(ids);
    }

    private Set<Long> findUserIdsByDisplayName(String term) {
        Set<Long> ids = new HashSet<>();
        userMapper.selectList(new LambdaQueryWrapper<User>()
                        .like(User::getUsername, term)
                        .or()
                        .like(User::getNickname, term))
                .forEach(user -> ids.add(user.getId()));
        userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                        .like(UserProfile::getNickname, term))
                .forEach(profile -> ids.add(profile.getUserId()));
        return ids;
    }
}
