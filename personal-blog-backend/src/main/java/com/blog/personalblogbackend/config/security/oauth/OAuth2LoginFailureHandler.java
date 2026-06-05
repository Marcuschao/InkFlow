package com.blog.personalblogbackend.config.security.oauth;

import com.blog.personalblogbackend.config.properties.OAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final OAuthProperties oAuthProperties;

    public OAuth2LoginFailureHandler(OAuthProperties oAuthProperties) {
        this.oAuthProperties = oAuthProperties;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String msg = exception.getMessage() != null ? exception.getMessage() : "oauth_failed";
        String base = oAuthProperties.getFrontendCallbackUrl();
        String sep = base.contains("?") ? "&" : "?";
        String target = base + sep + "error=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
