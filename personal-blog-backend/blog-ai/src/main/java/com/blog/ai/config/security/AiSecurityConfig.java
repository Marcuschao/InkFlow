package com.blog.ai.config.security;

import com.blog.ai.gateway.web.GatewayUserContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public SecurityFilterChain aiFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/internal/**", "/actuator/**").permitAll()
                        .requestMatchers("/api/agent/feedback/**").authenticated()
                        .requestMatchers("/api/agent/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(gatewayUserContextFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}
