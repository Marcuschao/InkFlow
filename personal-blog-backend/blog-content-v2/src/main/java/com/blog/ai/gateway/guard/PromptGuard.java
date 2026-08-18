package com.blog.ai.gateway.guard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.config.guard.SensitiveWordConfig;
import com.blog.ai.mapper.AiGuardRuleMapper;
import com.blog.ai.mapper.SensitiveWordMapper;
import com.blog.ai.model.entity.AiGuardRule;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PromptGuard {
    private final AiGuardRuleMapper aiGuardRuleMapper;
    private final SensitiveWordMapper sensitiveWordMapper;
    private final PromptInjectionGuard injectionGuard;
    private volatile SensitiveWordBs sensitiveWordBs;
    private volatile List<AiGuardRule> injectRules = List.of();
    private volatile List<AiGuardRule> outputRules = List.of();

    public PromptGuard(AiGuardRuleMapper aiGuardRuleMapper,
                       SensitiveWordMapper sensitiveWordMapper,
                       PromptInjectionGuard injectionGuard) {
        this.aiGuardRuleMapper = aiGuardRuleMapper;
        this.sensitiveWordMapper = sensitiveWordMapper;
        this.injectionGuard = injectionGuard;
    }

    @PostConstruct
    public void reload() {
        try {
            sensitiveWordBs = SensitiveWordConfig.build(sensitiveWordMapper);
        } catch (Exception e) {
            log.warn("Sensitive-word dictionary unavailable: {}", e.getMessage());
            sensitiveWordBs = SensitiveWordConfig.empty();
        }
        try {
            List<AiGuardRule> rows = aiGuardRuleMapper.selectList(
                    new LambdaQueryWrapper<AiGuardRule>().eq(AiGuardRule::getEnabled, 1));
            List<AiGuardRule> input = new ArrayList<>();
            List<AiGuardRule> output = new ArrayList<>();
            if (rows != null) for (AiGuardRule rule : rows) {
                if ("INJECT".equalsIgnoreCase(rule.getRuleType())) input.add(rule);
                if ("OUTPUT".equalsIgnoreCase(rule.getRuleType())) output.add(rule);
            }
            injectRules = List.copyOf(input);
            outputRules = List.copyOf(output);
        } catch (Exception e) {
            injectRules = List.of();
            outputRules = List.of();
            log.warn("Guard rules unavailable: {}", e.getMessage());
        }
    }

    public void checkInput(String userPrompt) {
        if (!StringUtils.hasText(userPrompt)) return;
        if (sensitiveWordBs != null && sensitiveWordBs.contains(userPrompt)) {
            throw new ServiceException(400, "输入包含不允许的内容");
        }
        for (AiGuardRule rule : injectRules) {
            if (matches(rule, userPrompt) && "BLOCK".equalsIgnoreCase(rule.getAction())) {
                throw new ServiceException(400, "输入触发安全策略");
            }
        }
        PromptInjectionGuard.Assessment assessment = injectionGuard.assess(userPrompt);
        if (assessment.riskLevel() != PromptInjectionGuard.RiskLevel.LOW) {
            log.warn("[guard] injection risk={} reason={} signals={} classifierFailed={}",
                    assessment.riskLevel(), assessment.reasonCode(), assessment.signals(), assessment.classifierFailed());
        }
        if (injectionGuard.enforce() && assessment.riskLevel() == PromptInjectionGuard.RiskLevel.HIGH) {
            throw new ServiceException(400, "请求包含高风险提示词注入行为");
        }
    }

    public String filterOutput(String content) {
        if (!StringUtils.hasText(content)) return content;
        String filtered = sensitiveWordBs == null ? content : sensitiveWordBs.replace(content);
        for (AiGuardRule rule : outputRules) {
            if (!matches(rule, filtered)) continue;
            if ("BLOCK".equalsIgnoreCase(rule.getAction())) return "回答触发安全策略，已停止输出。";
            if ("REDACT".equalsIgnoreCase(rule.getAction())) {
                filtered = Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE).matcher(filtered).replaceAll("***");
            }
        }
        return filtered;
    }

    private boolean matches(AiGuardRule rule, String text) {
        if (!StringUtils.hasText(rule.getPattern())) return false;
        try {
            return Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text).find();
        } catch (Exception e) {
            log.warn("Invalid guard rule id={}: {}", rule.getId(), e.getMessage());
            return false;
        }
    }
}
