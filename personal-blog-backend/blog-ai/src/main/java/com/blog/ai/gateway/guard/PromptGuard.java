package com.blog.ai.gateway.guard;

import com.blog.ai.config.guard.SensitiveWordConfig;
import com.blog.ai.mapper.AiGuardRuleMapper;
import com.blog.ai.mapper.SensitiveWordMapper;
import com.blog.ai.model.entity.AiGuardRule;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PromptGuard {

    private static final Logger log = LoggerFactory.getLogger(PromptGuard.class);

    private final AiGuardRuleMapper aiGuardRuleMapper;
    private final SensitiveWordMapper sensitiveWordMapper;
    private volatile SensitiveWordBs sensitiveWordBs;
    private volatile List<AiGuardRule> injectRules = List.of();
    private volatile List<AiGuardRule> outputRules = List.of();

    public PromptGuard(AiGuardRuleMapper aiGuardRuleMapper, SensitiveWordMapper sensitiveWordMapper) {
        this.aiGuardRuleMapper = aiGuardRuleMapper;
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    public void reload() {
        try {
            sensitiveWordBs = SensitiveWordConfig.build(sensitiveWordMapper);
        } catch (Exception e) {
            log.warn("敏感词库加载失败，使用空词库: {}", e.getMessage());
            sensitiveWordBs = SensitiveWordConfig.empty();
        }
        try {
            List<AiGuardRule> all = aiGuardRuleMapper.selectList(
                    new LambdaQueryWrapper<AiGuardRule>().eq(AiGuardRule::getEnabled, 1));
            List<AiGuardRule> inject = new ArrayList<>();
            List<AiGuardRule> output = new ArrayList<>();
            if (all != null) {
                for (AiGuardRule r : all) {
                    if ("INJECT".equalsIgnoreCase(r.getRuleType())) {
                        inject.add(r);
                    } else if ("OUTPUT".equalsIgnoreCase(r.getRuleType())) {
                        output.add(r);
                    }
                }
            }
            injectRules = inject;
            outputRules = output;
        } catch (Exception e) {
            injectRules = List.of();
            outputRules = List.of();
        }
    }

    public void checkInput(String userPrompt) {
    }

    public String filterOutput(String content) {
        return content;
    }
}
