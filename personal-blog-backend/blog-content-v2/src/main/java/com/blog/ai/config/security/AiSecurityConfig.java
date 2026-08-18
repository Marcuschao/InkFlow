package com.blog.ai.config.security;

import com.blog.ai.gateway.web.GatewayUserContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class AiSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GatewayUserContextFilter gatewayUserContextFilter;

    public AiSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                            GatewayUserContextFilter gatewayUserContextFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.gatewayUserContextFilter = gatewayUserContextFilter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain aiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(
                        "/internal/ai/**",
                        "/api/agent/**",
                        "/api/admin/articles/*/seo-ai",
                        "/api/admin/translations/**",
                        "/api/admin/freshness/**",
                        "/api/admin/reports/**",
                        "/api/admin/stats/**",
                        "/api/admin/knowledge/**",
                        "/api/admin/rag/**",
                        "/api/admin/ai/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/internal/ai/**").permitAll()
                        .requestMatchers("/api/agent/feedback/**").authenticated()
                        .requestMatchers("/api/agent/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(gatewayUserContextFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}
