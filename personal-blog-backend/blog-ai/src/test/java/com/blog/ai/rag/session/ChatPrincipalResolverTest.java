package com.blog.ai.rag.session;

import com.blog.ai.gateway.context.GatewayUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPrincipalResolverTest {
    private final ChatPrincipalResolver resolver = new ChatPrincipalResolver();

    @AfterEach
    void clearContext() {
        GatewayUserContext.clear();
    }

    @Test
    void issuesSiteWideHttpOnlyGuestCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        ChatPrincipal principal = resolver.resolve(request, response);

        assertThat(principal.userId()).isNull();
        assertThat(principal.guestTokenHash()).hasSize(64);
        assertThat(response.getHeader("Set-Cookie"))
                .contains("INKFLOW_GUEST_ID=")
                .contains("Path=/")
                .contains("Max-Age=2592000")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }

    @Test
    void preservesAuthenticatedUserAndHashesExistingCookie() {
        GatewayUserContext.set(42L, "tester");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(ChatPrincipalResolver.GUEST_COOKIE, "known-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        ChatPrincipal principal = resolver.resolve(request, response);

        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(principal.guestTokenHash()).isEqualTo(ChatPrincipalResolver.sha256("known-token"));
        assertThat(response.getHeader("Set-Cookie")).isNull();
    }
}
