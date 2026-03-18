package com.sism.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SISM WebSocket Handler
 * SISM WebSocket 处理器
 *
 * Handles WebSocket connections at /ws endpoint.
 * Manages session lifecycle and message routing.
 */
@Slf4j
@Component
public class SismWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();
    private final Map<String, String> userIdBySessionId = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        if (userId == null || userId.isBlank()) {
            log.warn("Rejecting WebSocket connection without userId: sessionId={}", session.getId());
            session.close(CloseStatus.BAD_DATA.withReason("Missing userId"));
            return;
        }

        sessionsByUserId
                .computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet())
                .add(session);
        userIdBySessionId.put(session.getId(), userId);
        log.info("WebSocket connected: sessionId={}, userId={}, remoteAddress={}",
                session.getId(), userId, session.getRemoteAddress());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received WebSocket message from session {}: {}", session.getId(), payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        unregisterSession(session);
        log.info("WebSocket disconnected: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: sessionId={}, error={}",
                session.getId(), exception.getMessage());
        unregisterSession(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * Broadcast message to all connected sessions
     */
    public void broadcast(NotificationMessagePayload message) {
        sessionsByUserId.keySet().forEach(userId -> sendToUser(userId, message));
    }

    public boolean sendToUser(String userId, NotificationMessagePayload message) {
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("No active WebSocket sessions for userId={}", userId);
            return false;
        }

        String payload = toJson(message);
        TextMessage textMessage = new TextMessage(payload);
        boolean delivered = false;

        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                    delivered = true;
                }
            } catch (IOException e) {
                log.error("Error sending WebSocket notification to session {}: {}", session.getId(), e.getMessage());
            }
        }

        return delivered;
    }

    /**
     * Get number of active connections
     */
    public int getActiveConnections() {
        return userIdBySessionId.size();
    }

    public int getActiveConnections(String userId) {
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        return sessions == null ? 0 : sessions.size();
    }

    private void unregisterSession(WebSocketSession session) {
        String userId = userIdBySessionId.remove(session.getId());
        if (userId == null) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUserId.remove(userId);
        }
    }

    private String extractUserId(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }

        return UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("userId");
    }

    private String toJson(NotificationMessagePayload message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize WebSocket notification payload", e);
        }
    }

    public record NotificationMessagePayload(
            String type,
            String title,
            String content,
            String entityType,
            Long entityId,
            Long approvalInstanceId,
            String stepName,
            String timestamp
    ) {
    }
}
