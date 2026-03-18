package com.sism.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

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

    public WebSocketConfig(SismWebSocketHandler sismWebSocketHandler) {
        this.sismWebSocketHandler = sismWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sismWebSocketHandler, "/ws/notifications")
                .setAllowedOriginPatterns("*");
    }
}
