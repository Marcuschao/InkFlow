package com.blog.ai.gateway.quota;

import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.gateway.AiTaskType;
import com.blog.ai.gateway.config.GatewayProperties;
import com.blog.ai.mapper.AiQuotaWhitelistMapper;
import com.blog.ai.model.entity.AiQuotaWhitelist;
import com.blog.ai.service.SiteKvService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TokenQuotaService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String KV_GLOBAL = "ai.quota.global";
    private static final String KV_USER = "ai.quota.user.default";

    private final RedissonClient redissonClient;
    private final GatewayProperties gatewayProperties;
    private final SiteKvService siteKvService;
    private final AiQuotaWhitelistMapper whitelistMapper;

    public TokenQuotaService(RedissonClient redissonClient,
                             GatewayProperties gatewayProperties,
                             SiteKvService siteKvService,
                             AiQuotaWhitelistMapper whitelistMapper) {
        this.redissonClient = redissonClient;
        this.gatewayProperties = gatewayProperties;
        this.siteKvService = siteKvService;
        this.whitelistMapper = whitelistMapper;
    }

    public void checkQuota(Long userId, AiTaskType taskType) {
        if (isWhitelisted(userId)) {
            return;
        }
        String day = today();
        long globalLimit = resolveGlobalLimit();
        long globalUsed = getCounter("token_quota:global:" + day);
        if (globalUsed >= globalLimit && !isCoreTask(taskType)) {
            throw new ServiceException(429, "全站今日 AI 额度已用完");
        }
        if (userId != null) {
            long userLimit = resolveUserLimit();
            long userUsed = getCounter("token_quota:user:" + userId + ":" + day);
            if (userUsed >= userLimit) {
                throw new ServiceException(429, "今日额度已用完");
            }
        }
    }

    public void recordUsage(Long userId, int tokens) {
        if (tokens <= 0) {
            return;
        }
        String day = today();
        incr("token_quota:global:" + day, tokens);
        if (userId != null && !isWhitelisted(userId)) {
            incr("token_quota:user:" + userId + ":" + day, tokens);
        }
    }

    public long getGlobalUsed() {
        return getCounter("token_quota:global:" + today());
    }

    public long getUserUsed(Long userId) {
        if (userId == null) {
            return 0;
        }
        return getCounter("token_quota:user:" + userId + ":" + today());
    }

    public long resolveGlobalLimit() {
        return siteKvService.get(KV_GLOBAL)
                .map(Long::parseLong)
                .orElse(gatewayProperties.getQuota().getGlobalDailyTokens());
    }

    public long resolveUserLimit() {
        return siteKvService.get(KV_USER)
                .map(Long::parseLong)
                .orElse(gatewayProperties.getQuota().getUserDailyTokens());
    }

    public void saveGlobalLimit(long limit) {
        siteKvService.put(KV_GLOBAL, String.valueOf(limit));
    }

    public void saveUserLimit(long limit) {
        siteKvService.put(KV_USER, String.valueOf(limit));
    }

    private boolean isCoreTask(AiTaskType taskType) {
        return gatewayProperties.getQuota().getCoreTaskTypes().stream()
                .anyMatch(t -> t.equalsIgnoreCase(taskType.code()));
    }

    private boolean isWhitelisted(Long userId) {
        if (userId == null) {
            return false;
        }
        if (gatewayProperties.getQuota().getWhitelistUserIds().contains(userId)) {
            return true;
        }
        Long cnt = whitelistMapper.selectCount(new LambdaQueryWrapper<AiQuotaWhitelist>()
                .eq(AiQuotaWhitelist::getUserId, userId));
        return cnt != null && cnt > 0;
    }

    private long getCounter(String key) {
        RAtomicLong atomic = redissonClient.getAtomicLong(key);
        return atomic.get();
    }

    private void incr(String key, long delta) {
        RAtomicLong atomic = redissonClient.getAtomicLong(key);
        atomic.addAndGet(delta);
        atomic.expire(Duration.ofHours(48));
    }

    private String today() {
        return LocalDate.now(ZONE).format(DAY_FMT);
    }

    public Set<Long> listWhitelistUserIds() {
        Set<Long> ids = new HashSet<>(gatewayProperties.getQuota().getWhitelistUserIds());
        List<AiQuotaWhitelist> rows = whitelistMapper.selectList(null);
        if (rows != null) {
            rows.forEach(r -> ids.add(r.getUserId()));
        }
        return ids;
    }
}
