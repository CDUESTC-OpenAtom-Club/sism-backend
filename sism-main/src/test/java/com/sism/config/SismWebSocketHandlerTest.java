package com.sism.config;

import com.sism.iam.application.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sism WebSocket Handler Tests")
class SismWebSocketHandlerTest {

    @Mock
    private WebSocketSession session;

    @Mock
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Should reject connection when userId is missing")
    void shouldRejectConnectionWithoutUserId() throws Exception {
        SismWebSocketHandler handler = new SismWebSocketHandler(new ObjectMapper(), jwtTokenService);

        when(session.getId()).thenReturn("session-1");
        when(session.getUri()).thenReturn(URI.create("ws://localhost:8080/ws/notifications"));

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA.withReason("Missing or invalid token"));
        assertEquals(0, handler.getActiveConnections());
    }

    @Test
    @DisplayName("Should register session and send payload to matching user")
    void shouldSendPayloadToConnectedUser() throws Exception {
        SismWebSocketHandler handler = new SismWebSocketHandler(new ObjectMapper(), jwtTokenService);
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);

        when(session.getId()).thenReturn("session-2");
        when(session.getUri()).thenReturn(URI.create("ws://localhost:8080/ws/notifications?token=valid-token"));
        when(session.isOpen()).thenReturn(true);
        when(jwtTokenService.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenService.getUserIdFromToken("valid-token")).thenReturn(124L);

        handler.afterConnectionEstablished(session);

        boolean delivered = handler.sendToUser("124", new SismWebSocketHandler.NotificationMessagePayload(
                "APPROVAL_REQUIRED",
                "Test title",
                "Test content",
                "plan",
                1L,
                2L,
                "审批节点",
                "2026-03-18T00:00:00Z"
        ));

        assertTrue(delivered);
        assertEquals(1, handler.getActiveConnections());
        assertEquals(1, handler.getActiveConnections("124"));
        verify(session).sendMessage(messageCaptor.capture());
        assertTrue(messageCaptor.getValue().getPayload().contains("\"type\":\"APPROVAL_REQUIRED\""));
        assertTrue(messageCaptor.getValue().getPayload().contains("\"title\":\"Test title\""));
    }

    @Test
    @DisplayName("Should cleanup closed sessions")
    void shouldCleanupClosedSessions() throws Exception {
        SismWebSocketHandler handler = new SismWebSocketHandler(new ObjectMapper(), jwtTokenService);

        when(session.getId()).thenReturn("session-3");
        when(session.getUri()).thenReturn(URI.create("ws://localhost:8080/ws/notifications?token=cleanup-token"));
        when(jwtTokenService.validateToken("cleanup-token")).thenReturn(true);
        when(jwtTokenService.getUserIdFromToken("cleanup-token")).thenReturn(200L);
        doReturn(false).when(session).isOpen();

        handler.afterConnectionEstablished(session);
        assertEquals(0, handler.getActiveConnections());
        assertEquals(0, handler.getActiveConnections("200"));
    }
}
