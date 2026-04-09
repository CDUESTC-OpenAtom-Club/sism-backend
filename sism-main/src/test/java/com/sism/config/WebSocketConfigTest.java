package com.sism.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocket Config Tests")
class WebSocketConfigTest {

    @Mock
    private SismWebSocketHandler sismWebSocketHandler;

    @Mock
    private WebSocketHandlerRegistry registry;

    @Mock
    private WebSocketHandlerRegistration registration;

    @Test
    @DisplayName("Should apply wildcard allowed origins")
    void shouldApplyWildcardAllowedOrigins() {
        WebSocketConfig config = new WebSocketConfig(sismWebSocketHandler);

        doReturn(registration).when(registry).addHandler(sismWebSocketHandler, "/ws/notifications");

        config.registerWebSocketHandlers(registry);

        verify(registration).setAllowedOriginPatterns("*");
    }
}
