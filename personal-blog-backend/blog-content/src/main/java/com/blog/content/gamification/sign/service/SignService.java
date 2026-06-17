package com.blog.content.gamification.sign.service;

import com.blog.content.gamification.sign.model.vo.SignCalendarVo;
import com.blog.content.gamification.sign.model.vo.SignResultVo;
import com.blog.content.gamification.sign.model.vo.SignStatusVo;

public interface SignService {

    SignResultVo sign(Long userId);

    SignStatusVo status(Long userId);

    SignCalendarVo calendar(Long userId, int year, int month);

    int calcStreakDays(Long userId);

    int calcTotalDays(Long userId);
}
