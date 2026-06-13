package com.blog.auth.config.security;

import com.blog.auth.config.security.oauth.CustomOAuth2UserService;
import com.blog.auth.config.security.oauth.OAuth2LoginFailureHandler;
import com.blog.auth.config.security.oauth.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectProvider<CustomOAuth2UserService> customOAuth2UserService;
    private final ObjectProvider<OAuth2LoginSuccessHandler> oAuth2LoginSuccessHandler;
    private final ObjectProvider<OAuth2LoginFailureHandler> oAuth2LoginFailureHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectProvider<CustomOAuth2UserService> customOAuth2UserService,
                          ObjectProvider<OAuth2LoginSuccessHandler> oAuth2LoginSuccessHandler,
                          ObjectProvider<OAuth2LoginFailureHandler> oAuth2LoginFailureHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CustomOAuth2UserService oauthUserService = customOAuth2UserService.getIfAvailable();
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        if (oauthUserService != null) {
            http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        } else {
            http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        }
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/auth/**", "/auth/**", "/api/captcha/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/user/{id:\\d+}").permitAll()
                .requestMatchers("/api/user/me/**", "/api/user/profile", "/api/user/avatar").authenticated()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated());
        if (oauthUserService != null) {
            OAuth2LoginSuccessHandler successHandler = oAuth2LoginSuccessHandler.getIfAvailable();
            OAuth2LoginFailureHandler failureHandler = oAuth2LoginFailureHandler.getIfAvailable();
            if (successHandler != null && failureHandler != null) {
                http.oauth2Login(o -> o
                        .userInfoEndpoint(u -> u.userService(oauthUserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler));
            }
        }
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
