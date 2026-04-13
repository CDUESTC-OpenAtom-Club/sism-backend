package com.sism.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/**
 * WebSocket Configuration
 * WebSocket 配置类
 *
 * Enables WebSocket support and registers handlers for /ws/** endpoints.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SismWebSocketHandler sismWebSocketHandler;
    private final List<String> allowedOriginPatterns;

    public WebSocketConfig(
            SismWebSocketHandler sismWebSocketHandler,
            @Value("${app.websocket.allowed-origin-patterns:http://localhost:3500,http://127.0.0.1:3500}") List<String> allowedOriginPatterns
    ) {
        this.sismWebSocketHandler = sismWebSocketHandler;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sismWebSocketHandler, "/ws/notifications")
                .setAllowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new));
    }
}
