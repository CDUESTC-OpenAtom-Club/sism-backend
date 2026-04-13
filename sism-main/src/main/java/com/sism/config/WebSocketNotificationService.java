package com.sism.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Minimal service used to push real-time notification payloads to frontend clients.
 */
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SismWebSocketHandler webSocketHandler;

    public boolean sendToUser(String userId,
                              String type,
                              String title,
                              String content,
                              String entityType,
                              Long entityId,
                              Long approvalInstanceId,
                              String stepName) {
        return webSocketHandler.sendToUser(userId, new SismWebSocketHandler.NotificationMessagePayload(
                type,
                title,
                content,
                entityType,
                entityId,
                approvalInstanceId,
                stepName,
                Instant.now().toString()
        ));
    }
}
