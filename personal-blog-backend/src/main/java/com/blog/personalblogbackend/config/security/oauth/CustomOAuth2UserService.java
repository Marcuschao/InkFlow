package com.blog.personalblogbackend.config.security.oauth;

import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.service.UserOAuthService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserOAuthService userOAuthService;

    public CustomOAuth2UserService(UserOAuthService userOAuthService) {
        this.userOAuthService = userOAuthService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"github".equals(registrationId)) {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }
        Long bindUserId = resolveBindUserId();
        try {
            return userOAuthService.resolveGithubUser(oauthUser.getAttributes(), bindUserId);
        } catch (ServiceException ex) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user", ex.getMessage(), null));
        }
    }

    private Long resolveBindUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String token = request.getParameter("oauth_bind");
        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("OAUTH_BIND".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        return userOAuthService.consumeBindToken(token);
    }
}
