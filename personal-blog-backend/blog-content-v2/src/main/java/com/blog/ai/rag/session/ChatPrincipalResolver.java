package com.blog.ai.rag.session;

import com.blog.ai.gateway.context.GatewayUserContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class ChatPrincipalResolver {
    public static final String GUEST_COOKIE = "INKFLOW_GUEST_ID";
    private static final Duration MAX_AGE = Duration.ofDays(30);
    private final SecureRandom secureRandom = new SecureRandom();

    public ChatPrincipal resolveCurrent() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            throw new IllegalStateException("Chat principal requires an HTTP request");
        }
        return resolve(attrs.getRequest(), attrs.getResponse());
    }

    public ChatPrincipal resolve(HttpServletRequest request, HttpServletResponse response) {
        String token = readCookie(request);
        if (!StringUtils.hasText(token)) {
            token = newToken();
            if (response != null) {
                ResponseCookie cookie = ResponseCookie.from(GUEST_COOKIE, token)
                        .httpOnly(true)
                        .secure(isSecure(request))
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(MAX_AGE)
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            }
        }
        return new ChatPrincipal(GatewayUserContext.getUserId(), sha256(token));
    }

    private String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (GUEST_COOKIE.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean isSecure(HttpServletRequest request) {
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
