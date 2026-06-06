package com.blog.personalblogbackend.config.security.oauth;

import com.blog.personalblogbackend.config.properties.OAuthProperties;
import com.blog.personalblogbackend.config.security.JwtUtils;
import com.blog.personalblogbackend.service.UserOAuthService;
import com.blog.personalblogbackend.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

@Configuration
@Lazy
@Conditional(OAuthConfiguredCondition.class)
public class GithubOAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(OAuthProperties oauthProperties) {
        String redirectUri = StringUtils.hasText(oauthProperties.getRedirectUri())
                ? oauthProperties.getRedirectUri()
                : "{baseUrl}/login/oauth2/code/{registrationId}";
        ClientRegistration github = ClientRegistration.withRegistrationId("github")
                .clientId(oauthProperties.getClientId())
                .clientSecret(oauthProperties.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope("read:user", "user:email")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build();
        return new InMemoryClientRegistrationRepository(github);
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build();
        DefaultOAuth2AuthorizedClientManager manager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientRepository);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

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
}
