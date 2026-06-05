package com.blog.personalblogbackend.config.security.oauth;

import com.blog.personalblogbackend.config.properties.OAuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

@Configuration
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
}
