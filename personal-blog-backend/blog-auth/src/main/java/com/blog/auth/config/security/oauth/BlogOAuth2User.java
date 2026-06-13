package com.blog.auth.config.security.oauth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class BlogOAuth2User implements OAuth2User {

    private final Long userId;
    private final String username;
    private final String role;
    private final Map<String, Object> attributes;

    public BlogOAuth2User(Long userId, String username, String role, Map<String, Object> attributes) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.attributes = attributes != null ? attributes : Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
