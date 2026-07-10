package com.blog.ai.service;

import com.blog.ai.common.support.PageResult;
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

import java.util.List;
import java.util.Map;

public interface AdminAiService {

    AiStatsOverviewDto overview();

    AiStatsTrendDto trend(int days);

    List<AiModelUsageDto> byModel();

    List<AiUserUsageDto> byUser(int limit);

    PageResult<AiCallLog> logs(long page, long size);

    List<Map<String, Object>> listModels();

    void updateModelEnabled(Long id, boolean enabled);

    List<AiModelHealthDto> modelHealth();

    AiQuotaDto getQuota();

    void saveQuota(long globalDaily, long userDaily);

    List<AiQuotaWhitelist> listWhitelist();

    void addWhitelist(Long userId, String remark);

    void removeWhitelist(Long id);

    List<AiGuardRule> listGuardRules();

    AiGuardRule addGuardRule(AiGuardRule rule);

    void updateGuardRule(AiGuardRule rule);

    void deleteGuardRule(Long id);
}
