package com.blog.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.common.exception.ServiceException;
import com.blog.content.mapper.UserMuteMapper;
import com.blog.content.model.entity.UserMute;
import com.blog.content.service.UserMuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class UserMuteServiceImpl implements UserMuteService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final UserMuteMapper userMuteMapper;

    @Override
    public boolean isMuted(Long userId) {
        LocalDateTime until = getMuteUntil(userId);
        return until != null && until.isAfter(LocalDateTime.now(ZONE));
    }

    @Override
    public LocalDateTime getMuteUntil(Long userId) {
        if (userId == null) {
            return null;
        }
        UserMute row = userMuteMapper.selectOne(new LambdaQueryWrapper<UserMute>()
                .eq(UserMute::getUserId, userId)
                .orderByDesc(UserMute::getId)
                .last("LIMIT 1"));
        if (row == null || row.getMuteUntil() == null) {
            return null;
        }
        if (row.getMuteUntil().isBefore(LocalDateTime.now(ZONE))) {
            return null;
        }
        return row.getMuteUntil();
    }

    @Override
    public void mute(Long userId, int minutes) {
        if (userId == null || minutes <= 0) {
            throw new ServiceException(400, "参数无效");
        }
        UserMute row = new UserMute();
        row.setUserId(userId);
        row.setMuteUntil(LocalDateTime.now(ZONE).plusMinutes(minutes));
        row.setCreateTime(LocalDateTime.now(ZONE));
        userMuteMapper.insert(row);
    }
}
