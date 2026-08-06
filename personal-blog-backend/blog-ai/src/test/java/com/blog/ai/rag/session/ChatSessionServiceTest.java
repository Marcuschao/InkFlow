package com.blog.ai.rag.session;

import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.config.properties.RagProperties;
import com.blog.ai.llm.AiService;
import com.blog.ai.mapper.ChatMessageMapper;
import com.blog.ai.mapper.ChatSessionMapper;
import com.blog.ai.model.entity.ChatSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatSessionServiceTest {
    private ChatSessionMapper sessionMapper;
    private ChatSessionService service;

    @BeforeEach
    void setUp() {
        sessionMapper = mock(ChatSessionMapper.class);
        service = new ChatSessionService(sessionMapper, mock(ChatMessageMapper.class), mock(AiService.class),
                new RagProperties(), new ObjectMapper());
    }

    @Test
    void deniesAnotherUsersSession() {
        ChatSession session = session(1L, 7L, null);
        when(sessionMapper.selectById(1L)).thenReturn(session);

        assertThatThrownBy(() -> service.ensureSession(1L, new ChatPrincipal(8L, "guest"), "q"))
                .isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(403);
        verify(sessionMapper, never()).claimOne(anyLong(), anyLong(), anyString());
    }

    @Test
    void oldGuestSessionWithoutHashIsInvalid() {
        when(sessionMapper.selectById(1L)).thenReturn(session(1L, null, null));
        assertThatThrownBy(() -> service.assertSessionReadable(1L, new ChatPrincipal(null, "new-hash")))
                .isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(403);
    }

    @Test
    void atomicallyClaimsMatchingGuestSession() {
        ChatSession guest = session(1L, null, "guest-hash");
        ChatSession owned = session(1L, 9L, null);
        when(sessionMapper.selectById(1L)).thenReturn(guest, owned);
        when(sessionMapper.claimOne(1L, 9L, "guest-hash")).thenReturn(1);

        ChatSession result = service.ensureSession(1L, new ChatPrincipal(9L, "guest-hash"), "q");

        assertThat(result.getUserId()).isEqualTo(9L);
        verify(sessionMapper).claimOne(1L, 9L, "guest-hash");
    }

    @Test
    void zeroRowCasMustDenyAccess() {
        when(sessionMapper.selectById(1L)).thenReturn(session(1L, null, "guest-hash"));
        when(sessionMapper.claimOne(1L, 9L, "guest-hash")).thenReturn(0);

        assertThatThrownBy(() -> service.ensureSession(1L, new ChatPrincipal(9L, "guest-hash"), "q"))
                .isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(403);
    }

    private ChatSession session(Long id, Long userId, String guestHash) {
        ChatSession session = new ChatSession();
        session.setId(id);
        session.setUserId(userId);
        session.setGuestTokenHash(guestHash);
        return session;
    }
}
