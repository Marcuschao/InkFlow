package com.blog.auth.service;

import com.blog.auth.config.security.oauth.BlogOAuth2User;
import com.blog.auth.model.vo.user.UserOauthBindingVo;

import java.util.List;
import java.util.Map;

public interface UserOAuthService {

    BlogOAuth2User resolveGithubUser(Map<String, Object> attributes, Long bindUserId);

    List<UserOauthBindingVo> listBindings(Long userId);

    String createBindToken(Long userId);

    Long consumeBindToken(String token);

    void unbindGithub(Long userId);
}
