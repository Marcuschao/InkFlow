package com.blog.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Data
@ConfigurationProperties(prefix = "blog.oauth")
public class OAuthProperties {
    private boolean enabled = false;
    private String clientId = "";
    private String clientSecret = "";
    private String frontendCallbackUrl = "http://localhost:5173/oauth/callback";
    private String redirectUri = "";

    public boolean isConfigured() {
        return enabled && StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
    }
}
