package com.blog.auth.config.security.oauth;

import com.blog.auth.config.properties.OAuthProperties;
import com.blog.common.security.JwtUtils;
import com.blog.auth.service.UserOAuthService;
import com.blog.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final OAuthProperties oAuthProperties;
    private final UserService userService;
    private final UserOAuthService userOAuthService;

    public OAuth2LoginSuccessHandler(JwtUtils jwtUtils, OAuthProperties oAuthProperties, UserService userService,
                                       UserOAuthService userOAuthService) {
        this.jwtUtils = jwtUtils;
        this.oAuthProperties = oAuthProperties;
        this.userService = userService;
        this.userOAuthService = userOAuthService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        BlogOAuth2User principal = resolvePrincipal(authentication);
        String clientIp = request.getRemoteAddr();
        userService.recordLogin(principal.getUserId(), clientIp);

        String token = jwtUtils.generateToken(
                principal.getUserId(),
                principal.getUsername(),
                principal.getRole(),
                false);

        String base = oAuthProperties.getFrontendCallbackUrl();
        String sep = base.contains("?") ? "&" : "?";
        String target = base + sep
                + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&role=" + URLEncoder.encode(principal.getRole(), StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, target);
    }

    private BlogOAuth2User resolvePrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof BlogOAuth2User blogUser) {
            return blogUser;
        }
        if (principal instanceof OAuth2User oauth2User) {
            return userOAuthService.resolveGithubUser(oauth2User.getAttributes(), null);
        }
        throw new IllegalStateException("Unexpected OAuth2 principal: " + principal.getClass().getName());
    }
}
