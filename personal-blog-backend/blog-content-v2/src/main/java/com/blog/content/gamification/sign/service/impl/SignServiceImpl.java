package com.blog.content.gamification.sign.service.impl;

import com.blog.content.gamification.event.ActivityType;
import com.blog.content.gamification.event.UserActivityEventPublisher;
import com.blog.content.gamification.points.service.PointsService;
import com.blog.content.gamification.sign.model.vo.SignCalendarVo;
import com.blog.content.gamification.sign.model.vo.SignResultVo;
import com.blog.content.gamification.sign.model.vo.SignStatusVo;
import com.blog.content.gamification.sign.service.SignService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SignServiceImpl implements SignService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String BITMAP_PREFIX = "sign:bitmap:";
    private static final String TOTAL_PREFIX = "sign:total:";

    private final StringRedisTemplate redisTemplate;
    private final PointsService pointsService;
    private final UserActivityEventPublisher activityEventPublisher;

    public SignServiceImpl(StringRedisTemplate redisTemplate,
                           PointsService pointsService,
                           UserActivityEventPublisher activityEventPublisher) {
        this.redisTemplate = redisTemplate;
        this.pointsService = pointsService;
        this.activityEventPublisher = activityEventPublisher;
    }

    @Override
    public SignResultVo sign(Long userId) {
        LocalDate today = LocalDate.now();
        String key = bitmapKey(userId, today);
        int offset = today.getDayOfMonth() - 1;
        Boolean old = redisTemplate.opsForValue().setBit(key, offset, true);
        if (Boolean.TRUE.equals(old)) {
            int streak = calcStreakDays(userId);
            int total = calcTotalDays(userId);
            return new SignResultVo(true, true, streak, total, 0);
        }
        redisTemplate.opsForValue().increment(totalKey(userId));

        int pointsEarned = 5;
        pointsService.addPoints(userId, 5, "每日签到");

        int streak = calcStreakDays(userId);
        if (streak == 7) {
            pointsService.addPoints(userId, 10, "连续签到7天奖励");
            pointsEarned += 10;
        } else if (streak == 30) {
            pointsService.addPoints(userId, 50, "连续签到30天奖励");
            pointsEarned += 50;
        }

        int total = calcTotalDays(userId);
        activityEventPublisher.publish(ActivityType.SIGN_COMPLETED, userId,
                Map.of("streak", streak, "total", total));

        return new SignResultVo(true, false, streak, total, pointsEarned);
    }

    @Override
    public SignStatusVo status(Long userId) {
        LocalDate today = LocalDate.now();
        boolean signedToday = isSigned(userId, today);
        int streak = calcStreakDays(userId);
        int total = calcTotalDays(userId);
        int nextBonusDays;
        int nextBonusPoints;
        if (streak < 7) {
            nextBonusDays = 7 - streak;
            nextBonusPoints = 10;
        } else if (streak < 30) {
            nextBonusDays = 30 - streak;
            nextBonusPoints = 50;
        } else {
            nextBonusDays = 0;
            nextBonusPoints = 0;
        }
        return new SignStatusVo(signedToday, streak, total, nextBonusDays, nextBonusPoints);
    }

    @Override
    public SignCalendarVo calendar(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate date = ym.atDay(1);
        String key = bitmapKey(userId, date);
        int daysInMonth = ym.lengthOfMonth();
        List<Boolean> days = new ArrayList<>(daysInMonth);
        int signedCount = 0;
        for (int i = 0; i < daysInMonth; i++) {
            boolean signed = Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(key, i));
            days.add(signed);
            if (signed) {
                signedCount++;
            }
        }
        return new SignCalendarVo(year, month, days, signedCount);
    }

    @Override
    public int calcStreakDays(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate date = today;
        if (!isSigned(userId, date)) {
            date = date.minusDays(1);
        }
        int streak = 0;
        while (isSigned(userId, date)) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }

    @Override
    public int calcTotalDays(Long userId) {
        String val = redisTemplate.opsForValue().get(totalKey(userId));
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private boolean isSigned(Long userId, LocalDate date) {
        String key = bitmapKey(userId, date);
        int offset = date.getDayOfMonth() - 1;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(key, offset));
    }

    private String bitmapKey(Long userId, LocalDate date) {
        return BITMAP_PREFIX + userId + ":" + date.format(MONTH_FMT);
    }

    private String totalKey(Long userId) {
        return TOTAL_PREFIX + userId;
    }
}
