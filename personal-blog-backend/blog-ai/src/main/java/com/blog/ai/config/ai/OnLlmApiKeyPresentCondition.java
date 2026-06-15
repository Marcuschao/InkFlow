package com.blog.ai.config.ai;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class OnLlmApiKeyPresentCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String key = context.getEnvironment().getProperty("spring.ai.openai.api-key");
        return StringUtils.hasText(key);
    }
}
