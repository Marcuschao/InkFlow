package com.blog.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.common.support.PageResult;
import com.blog.ai.gateway.config.AiApiKeyCipher;
import com.blog.ai.gateway.config.GatewayProperties;
import com.blog.ai.gateway.config.ModelProviderConfig;
import com.blog.ai.gateway.guard.PromptGuard;
import com.blog.ai.gateway.health.ModelHealthChecker;
import com.blog.ai.gateway.health.ProviderHealthState;
import com.blog.ai.gateway.quota.TokenQuotaService;
import com.blog.ai.gateway.router.ModelRouter;
import com.blog.ai.mapper.AiCallLogMapper;
import com.blog.ai.mapper.AiGuardRuleMapper;
import com.blog.ai.mapper.AiModelConfigMapper;
import com.blog.ai.mapper.AiQuotaWhitelistMapper;
import com.blog.ai.model.dto.ai.AiModelCreateRequest;
import com.blog.ai.model.dto.ai.AiModelHealthDto;
import com.blog.ai.model.dto.ai.AiModelUsageDto;
import com.blog.ai.model.dto.ai.AiQuotaDto;
import com.blog.ai.model.dto.ai.AiStatsOverviewDto;
import com.blog.ai.model.dto.ai.AiStatsTrendDto;
import com.blog.ai.model.dto.ai.AiUserUsageDto;
import com.blog.ai.model.entity.AiCallLog;
import com.blog.ai.model.entity.AiGuardRule;
import com.blog.ai.model.entity.AiModelConfig;
import com.blog.ai.model.entity.AiQuotaWhitelist;
import com.blog.ai.service.AdminAiService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminAiServiceImpl implements AdminAiService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final AiCallLogMapper aiCallLogMapper;
    private final AiModelConfigMapper aiModelConfigMapper;
    private final AiQuotaWhitelistMapper whitelistMapper;
    private final AiGuardRuleMapper guardRuleMapper;
    private final TokenQuotaService tokenQuotaService;
    private final ModelHealthChecker healthChecker;
    private final ModelProviderConfig modelProviderConfig;
    private final GatewayProperties gatewayProperties;
    private final ModelRouter modelRouter;
    private final PromptGuard promptGuard;
    private final AiApiKeyCipher apiKeyCipher;

    public AdminAiServiceImpl(AiCallLogMapper aiCallLogMapper,
                              AiModelConfigMapper aiModelConfigMapper,
                              AiQuotaWhitelistMapper whitelistMapper,
                              AiGuardRuleMapper guardRuleMapper,
                              TokenQuotaService tokenQuotaService,
                              ModelHealthChecker healthChecker,
                              ModelProviderConfig modelProviderConfig,
                              GatewayProperties gatewayProperties,
                              ModelRouter modelRouter,
                              PromptGuard promptGuard,
                              AiApiKeyCipher apiKeyCipher) {
        this.aiCallLogMapper = aiCallLogMapper;
        this.aiModelConfigMapper = aiModelConfigMapper;
        this.whitelistMapper = whitelistMapper;
        this.guardRuleMapper = guardRuleMapper;
        this.tokenQuotaService = tokenQuotaService;
        this.healthChecker = healthChecker;
        this.modelProviderConfig = modelProviderConfig;
        this.gatewayProperties = gatewayProperties;
        this.modelRouter = modelRouter;
        this.promptGuard = promptGuard;
        this.apiKeyCipher = apiKeyCipher;
    }

    @Override
    public AiStatsOverviewDto overview() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        Map<String, Object> row = aiCallLogMapper.overview(start, end);
        AiStatsOverviewDto dto = new AiStatsOverviewDto();
        long cnt = toLong(row.get("cnt"));
        long ok = toLong(row.get("ok_cnt"));
        dto.setTodayCalls(cnt);
        dto.setSuccessRate(cnt > 0 ? ok * 100.0 / cnt : 0);
        dto.setAvgLatencyMs(toDouble(row.get("avg_latency")));
        dto.setTotalCost(toDouble(row.get("total_cost")));
        return dto;
    }

    @Override
    public AiStatsTrendDto trend(int days) {
        int d = Math.min(Math.max(days, 1), 90);
        LocalDate end = LocalDate.now(ZONE);
        LocalDate start = end.minusDays(d - 1L);
        List<Map<String, Object>> rows = aiCallLogMapper.trendByDay(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        Map<LocalDate, Map<String, Object>> byDay = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                LocalDate ld = toLocalDate(row.get("stat_day"));
                if (ld != null) {
                    byDay.put(ld, row);
                }
            }
        }
        AiStatsTrendDto dto = new AiStatsTrendDto();
        List<String> labels = new ArrayList<>();
        List<Long> calls = new ArrayList<>();
        List<Double> costs = new ArrayList<>();
        List<Double> rates = new ArrayList<>();
        for (LocalDate cur = start; !cur.isAfter(end); cur = cur.plusDays(1)) {
            labels.add(cur.toString());
            Map<String, Object> row = byDay.get(cur);
            long cnt = row == null ? 0 : toLong(row.get("cnt"));
            long ok = row == null ? 0 : toLong(row.get("ok_cnt"));
            calls.add(cnt);
            costs.add(row == null ? 0 : toDouble(row.get("total_cost")));
            rates.add(cnt > 0 ? ok * 100.0 / cnt : 0);
        }
        dto.setLabels(labels);
        dto.setCalls(calls);
        dto.setCosts(costs);
        dto.setSuccessRates(rates);
        return dto;
    }

    @Override
    public List<AiModelUsageDto> byModel() {
        LocalDate today = LocalDate.now(ZONE);
        List<Map<String, Object>> rows = aiCallLogMapper.byModel(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        List<AiModelUsageDto> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            out.add(new AiModelUsageDto(String.valueOf(row.get("model")), toLong(row.get("cnt"))));
        }
        return out;
    }

    @Override
    public List<AiUserUsageDto> byUser(int limit) {
        LocalDate today = LocalDate.now(ZONE);
        List<Map<String, Object>> rows = aiCallLogMapper.byUser(today.atStartOfDay(), today.plusDays(1).atStartOfDay(), limit);
        List<AiUserUsageDto> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            Object uid = row.get("user_id");
            Long userId = uid instanceof Number n ? n.longValue() : null;
            out.add(new AiUserUsageDto(userId, row.get("username") != null ? String.valueOf(row.get("username")) : null,
                    toLong(row.get("cnt"))));
        }
        return out;
    }

    @Override
    public PageResult<AiCallLog> logs(long page, long size) {
        Page<AiCallLog> p = aiCallLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AiCallLog>().orderByDesc(AiCallLog::getId));
        return PageResult.build(p);
    }

    @Override
    public List<Map<String, Object>> listModels() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (GatewayProperties.ProviderDef def : gatewayProperties.getProviders()) {
            Map<String, Object> m = new HashMap<>();
            m.put("providerId", def.getId());
            m.put("name", def.getName());
            m.put("enabled", def.isEnabled());
            m.put("models", def.getModels());
            m.put("priority", def.getPriority());
            ProviderHealthState hs = healthChecker.getState(def.getId());
            m.put("health", hs.getStatus().name());
            out.add(m);
        }
        List<AiModelConfig> dbRows = aiModelConfigMapper.selectList(null);
        if (dbRows != null) {
            for (AiModelConfig row : dbRows) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", row.getId());
                m.put("providerId", row.getProviderId());
                m.put("model", row.getModel());
                m.put("enabled", row.getEnabled() != null && row.getEnabled() == 1);
                m.put("priority", row.getPriority());
                ProviderHealthState hs = healthChecker.getState(row.getProviderId());
                m.put("health", hs.getStatus().name());
                out.add(m);
            }
        }
        return out;
    }

    @Override
    public void updateModelEnabled(Long id, boolean enabled) {
        AiModelConfig row = aiModelConfigMapper.selectById(id);
        if (row == null) {
            return;
        }
        row.setEnabled(enabled ? 1 : 0);
        aiModelConfigMapper.updateById(row);
        modelProviderConfig.reload();
    }

    @Override
    public AiModelConfig addModel(AiModelCreateRequest req) {
        AiModelConfig existing = aiModelConfigMapper.selectOne(
                new LambdaQueryWrapper<AiModelConfig>().eq(AiModelConfig::getProviderId, req.getProviderId()));
        if (existing != null) {
            throw new ServiceException("providerId已存在: " + req.getProviderId());
        }
        AiModelConfig row = new AiModelConfig();
        row.setProviderId(req.getProviderId());
        row.setName(req.getName());
        row.setApiKey(apiKeyCipher.encrypt(req.getApiKey()));
        row.setBaseUrl(req.getBaseUrl());
        String modelsStr = req.getModels();
        row.setModels(modelsStr);
        row.setModel(modelsStr.split(",")[0].trim());
        row.setPriority(req.getPriority() != null ? req.getPriority() : 1);
        row.setMaxConcurrency(req.getMaxConcurrency() != null ? req.getMaxConcurrency() : 10);
        row.setTimeoutMs(req.getTimeoutMs() != null ? req.getTimeoutMs() : 5000L);
        row.setEnabled(req.getEnabled() == null || req.getEnabled() ? 1 : 0);
        row.setUpdatedAt(LocalDateTime.now());
        aiModelConfigMapper.insert(row);
        modelProviderConfig.reload();
        return row;
    }

    @Override
    public void deleteModel(Long id) {
        AiModelConfig row = aiModelConfigMapper.selectById(id);
        if (row == null) {
            return;
        }
        aiModelConfigMapper.deleteById(id);
        modelProviderConfig.reload();
    }

    @Override
    public List<AiModelHealthDto> modelHealth() {
        List<AiModelHealthDto> out = new ArrayList<>();
        for (Map.Entry<String, ProviderHealthState> e : healthChecker.snapshot().entrySet()) {
            AiModelHealthDto dto = new AiModelHealthDto();
            dto.setProviderId(e.getKey());
            dto.setStatus(e.getValue().getStatus().name());
            dto.setConsecutiveFailures(e.getValue().getConsecutiveFailures());
            dto.setLastError(e.getValue().getLastError());
            out.add(dto);
        }
        return out;
    }

    @Override
    public AiQuotaDto getQuota() {
        AiQuotaDto dto = new AiQuotaDto();
        dto.setGlobalDailyTokens(tokenQuotaService.resolveGlobalLimit());
        dto.setUserDailyTokens(tokenQuotaService.resolveUserLimit());
        dto.setGlobalUsed(tokenQuotaService.getGlobalUsed());
        return dto;
    }

    @Override
    public void saveQuota(long globalDaily, long userDaily) {
        tokenQuotaService.saveGlobalLimit(globalDaily);
        tokenQuotaService.saveUserLimit(userDaily);
    }

    @Override
    public List<AiQuotaWhitelist> listWhitelist() {
        return whitelistMapper.selectList(new LambdaQueryWrapper<AiQuotaWhitelist>().orderByDesc(AiQuotaWhitelist::getId));
    }

    @Override
    public void addWhitelist(Long userId, String remark) {
        AiQuotaWhitelist row = new AiQuotaWhitelist();
        row.setUserId(userId);
        row.setRemark(remark);
        whitelistMapper.insert(row);
    }

    @Override
    public void removeWhitelist(Long id) {
        whitelistMapper.deleteById(id);
    }

    @Override
    public List<AiGuardRule> listGuardRules() {
        return guardRuleMapper.selectList(new LambdaQueryWrapper<AiGuardRule>().orderByDesc(AiGuardRule::getId));
    }

    @Override
    public AiGuardRule addGuardRule(AiGuardRule rule) {
        guardRuleMapper.insert(rule);
        promptGuard.reload();
        return rule;
    }

    @Override
    public void updateGuardRule(AiGuardRule rule) {
        guardRuleMapper.updateById(rule);
        promptGuard.reload();
    }

    @Override
    public void deleteGuardRule(Long id) {
        guardRuleMapper.deleteById(id);
        promptGuard.reload();
    }

    private static long toLong(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private static double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private static LocalDate toLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate ld) return ld;
        if (o instanceof java.sql.Date sd) return sd.toLocalDate();
        try {
            return LocalDate.parse(o.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
