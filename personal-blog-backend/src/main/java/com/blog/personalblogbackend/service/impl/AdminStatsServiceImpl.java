package com.blog.personalblogbackend.service.impl;

import com.blog.personalblogbackend.dto.stat.AiUsageItemDto;
import com.blog.personalblogbackend.dto.stat.PvTrendDto;
import com.blog.personalblogbackend.dto.stat.StatsSummaryDto;
import com.blog.personalblogbackend.dto.stat.TopArticleStatDto;
import com.blog.personalblogbackend.entity.Article;
import com.blog.personalblogbackend.mapper.AiCallLogMapper;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.mapper.PageViewEventMapper;
import com.blog.personalblogbackend.service.AdminStatsService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminStatsServiceImpl implements AdminStatsService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private PageViewEventMapper pageViewEventMapper;
    @Autowired
    private AiCallLogMapper aiCallLogMapper;

    @Override
    public StatsSummaryDto summary() {
        Long ac = articleMapper.selectCount(new QueryWrapper<Article>().eq("status", 1));
        Long pv = pageViewEventMapper.countTotalPv();
        Long uv = pageViewEventMapper.countDistinctVisitors();
        Long ai = aiCallLogMapper.countTotal();
        long articleCount = ac == null ? 0 : ac;
        long pvTotal = pv == null ? 0 : pv;
        long uvEstimate = uv == null ? 0 : uv;
        long aiCallTotal = ai == null ? 0 : ai;
        return new StatsSummaryDto(articleCount, pvTotal, uvEstimate, aiCallTotal);
    }

    @Override
    public PvTrendDto pvTrend(int days) {
        int d = Math.min(Math.max(days, 1), 90);
        LocalDate end = LocalDate.now(ZONE);
        LocalDate start = end.minusDays(d - 1L);
        LocalDateTime startDt = start.atStartOfDay();
        List<Map<String, Object>> rows = pageViewEventMapper.aggregatePvByDay(startDt);
        Map<LocalDate, Long> byDay = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                LocalDate ld = toLocalDate(row.get("stat_day"));
                long n = toLong(row.get("pv_cnt"));
                if (ld != null) {
                    byDay.put(ld, n);
                }
            }
        }
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (LocalDate cur = start; !cur.isAfter(end); cur = cur.plusDays(1)) {
            labels.add(cur.toString());
            values.add(byDay.getOrDefault(cur, 0L));
        }
        return new PvTrendDto(labels, values);
    }

    @Override
    public List<TopArticleStatDto> topArticles(int limit) {
        int lim = Math.min(Math.max(limit, 1), 50);
        List<Article> list = articleMapper.selectList(new QueryWrapper<Article>()
                .eq("status", 1)
                .orderByDesc("view_count")
                .last("LIMIT " + lim));
        if (list == null) {
            return List.of();
        }
        List<TopArticleStatDto> out = new ArrayList<>();
        for (Article a : list) {
            Integer vc = a.getViewCount() != null ? a.getViewCount() : 0;
            out.add(new TopArticleStatDto(a.getId(), a.getTitle(), vc));
        }
        return out;
    }

    @Override
    public List<AiUsageItemDto> aiUsage(String period) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime start;
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        if ("yesterday".equalsIgnoreCase(period)) {
            LocalDate y = today.minusDays(1);
            start = y.atStartOfDay();
            end = today.atStartOfDay();
        } else {
            start = today.minusDays(6).atStartOfDay();
        }
        List<Map<String, Object>> rows = aiCallLogMapper.aggregateByFeature(start, end);
        List<AiUsageItemDto> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            Object f = row.get("feat");
            Object c = row.get("cnt");
            if (f != null) {
                out.add(new AiUsageItemDto(String.valueOf(f), toLong(c)));
            }
        }
        return out;
    }

    private static LocalDate toLocalDate(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDate ld) {
            return ld;
        }
        if (o instanceof java.sql.Date sd) {
            return sd.toLocalDate();
        }
        if (o instanceof java.util.Date ud) {
            return new java.sql.Date(ud.getTime()).toLocalDate();
        }
        try {
            return LocalDate.parse(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (Exception e) {
            return 0L;
        }
    }
}
