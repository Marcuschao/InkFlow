package com.blog.personalblogbackend.config.security.oauth;

import com.blog.personalblogbackend.config.properties.OAuthProperties;
import com.blog.personalblogbackend.config.security.JwtUtils;
import com.blog.personalblogbackend.service.UserOAuthService;
import com.blog.personalblogbackend.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Order(1)
@Conditional(OAuthConfiguredCondition.class)
public class OAuth2SecurityConfig {

    @Bean
    public CustomOAuth2UserService customOAuth2UserService(UserOAuthService userOAuthService) {
        return new CustomOAuth2UserService(userOAuthService);
    }

    @Bean
    public OAuth2LoginSuccessHandler oauth2LoginSuccessHandler(JwtUtils jwtUtils, OAuthProperties oAuthProperties,
                                                               UserService userService, UserOAuthService userOAuthService) {
        return new OAuth2LoginSuccessHandler(jwtUtils, oAuthProperties, userService, userOAuthService);
    }

    @Bean
    public OAuth2LoginFailureHandler oauth2LoginFailureHandler(OAuthProperties oAuthProperties) {
        return new OAuth2LoginFailureHandler(oAuthProperties);
    }

    @Bean
    public SecurityFilterChain oauth2FilterChain(
            HttpSecurity http,
            CustomOAuth2UserService oauthUserService,
            OAuth2LoginSuccessHandler successHandler,
            OAuth2LoginFailureHandler failureHandler) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauthUserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler));
        return http.build();
    }
}
