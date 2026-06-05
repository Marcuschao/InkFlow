package com.blog.personalblogbackend.config.security.oauth;

import com.blog.personalblogbackend.config.properties.OAuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthConfiguration {
}
