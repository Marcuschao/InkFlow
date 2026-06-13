package com.blog.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.security.jwt")
public class JwtProperties {
    private String secret = "MyUltraSecureJWTSecretForProductionEnvironment2026!@#";
    private Long expiration = 43200L;
    private Long rememberMeExpiration = 604800L;
}
