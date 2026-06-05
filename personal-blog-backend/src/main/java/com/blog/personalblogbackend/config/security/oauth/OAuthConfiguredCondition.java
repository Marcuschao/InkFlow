package com.blog.personalblogbackend.config.security.oauth;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class OAuthConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var env = context.getEnvironment();
        boolean enabled = env.getProperty("blog.oauth.enabled", Boolean.class, false);
        String clientId = env.getProperty("blog.oauth.client-id");
        String clientSecret = env.getProperty("blog.oauth.client-secret");
        return enabled && StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
    }
}
