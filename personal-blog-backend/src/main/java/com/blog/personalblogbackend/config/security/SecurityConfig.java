package com.blog.personalblogbackend.config.security;

import com.blog.personalblogbackend.config.security.oauth.CustomOAuth2UserService;
import com.blog.personalblogbackend.config.security.oauth.OAuth2LoginFailureHandler;
import com.blog.personalblogbackend.config.security.oauth.OAuth2LoginSuccessHandler;
import com.blog.personalblogbackend.concurrency.ApiRateLimitFilter;
import com.blog.personalblogbackend.monitor.TraceIdFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
    private final ApiRateLimitFilter apiRateLimitFilter;
    private final TraceIdFilter traceIdFilter;
    private final ObjectProvider<CustomOAuth2UserService> customOAuth2UserService;
    private final ObjectProvider<OAuth2LoginSuccessHandler> oAuth2LoginSuccessHandler;
    private final ObjectProvider<OAuth2LoginFailureHandler> oAuth2LoginFailureHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ApiRateLimitFilter apiRateLimitFilter,
                          TraceIdFilter traceIdFilter,
                          ObjectProvider<CustomOAuth2UserService> customOAuth2UserService,
                          ObjectProvider<OAuth2LoginSuccessHandler> oAuth2LoginSuccessHandler,
                          ObjectProvider<OAuth2LoginFailureHandler> oAuth2LoginFailureHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiRateLimitFilter = apiRateLimitFilter;
        this.traceIdFilter = traceIdFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CustomOAuth2UserService oauthUserService = customOAuth2UserService.getIfAvailable();

        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        if (oauthUserService != null) {
            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        } else {
            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        }

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/health", "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/auth/**", "/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles/*/versions").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/articles/*/versions/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/articles/*/like/status").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/articles/*/favorite/status").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/articles/*/like").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/articles/*/favorite").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/tags/**", "/tags/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**", "/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/about", "/about").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/site/**", "/site/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/search/**", "/search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/hot-search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/links", "/api/links/**", "/links/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/captcha/**", "/captcha/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/subscribe", "/subscribe").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/subscribe/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/push/vapid-public-key").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/push/subscribe", "/api/push/unsubscribe").permitAll()
                .requestMatchers(HttpMethod.GET, "/rss.xml", "/sitemap.xml", "/robots.txt").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/stat/view").permitAll()
                .requestMatchers(HttpMethod.GET, "/upload/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/diary/public", "/api/diary/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/chat/history", "/api/chat/online-users").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/chat/send", "/api/chat/presence", "/api/chat/offline", "/api/chat/recall/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/chat/mute-status").authenticated()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles/*/likes/count").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles/**", "/articles/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/user/feed").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/user/me/favorites").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/user/{id:\\d+}/followers").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/user/{id:\\d+}/following").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/user/{id:\\d+}/follow/status").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/user/{id:\\d+}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/user/{id:\\d+}/follow").authenticated()
                .requestMatchers("/api/user/me/oauth/**").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()
                .requestMatchers("/api/admin/**", "/admin/**").hasRole("ADMIN")
                .requestMatchers("/actuator/**").authenticated()
                .anyRequest().authenticated()
        );

        if (oauthUserService != null) {
            OAuth2LoginSuccessHandler successHandler = oAuth2LoginSuccessHandler.getIfAvailable();
            OAuth2LoginFailureHandler failureHandler = oAuth2LoginFailureHandler.getIfAvailable();
            if (successHandler != null && failureHandler != null) {
                http.oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauthUserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler));
            }
        }

        http
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiRateLimitFilter, JwtAuthenticationFilter.class)
                .addFilterBefore(traceIdFilter, ApiRateLimitFilter.class);

        return http.build();
    }
}
