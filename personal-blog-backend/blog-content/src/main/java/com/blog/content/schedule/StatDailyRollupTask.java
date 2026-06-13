package com.blog.content.schedule;

import com.blog.content.model.entity.StatDaily;
import com.blog.content.mapper.PageViewEventMapper;
import com.blog.content.mapper.StatDailyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class StatDailyRollupTask {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private PageViewEventMapper pageViewEventMapper;
    @Autowired
    private StatDailyMapper statDailyMapper;

    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Shanghai")
    public void rollupYesterday() {
        LocalDate day = LocalDate.now(ZONE).minusDays(1);
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        Long pv = pageViewEventMapper.countPvBetween(start, end);
        Long uv = pageViewEventMapper.countUvBetween(start, end);
        StatDaily row = statDailyMapper.selectById(day);
        StatDaily sd = new StatDaily();
        sd.setStatDate(day);
        sd.setPvCount(pv == null ? 0L : pv);
        sd.setUvCount(uv == null ? 0L : uv);
        sd.setUpdatedAt(LocalDateTime.now());
        if (row == null) {
            statDailyMapper.insert(sd);
        } else {
            statDailyMapper.updateById(sd);
        }
    }

    @Scheduled(cron = "0 15 3 * * ?", zone = "Asia/Shanghai")
    public void purgeOldEvents() {
        LocalDateTime before = LocalDateTime.now(ZONE).minusDays(90);
        pageViewEventMapper.deleteOlderThan(before);
    }
}
