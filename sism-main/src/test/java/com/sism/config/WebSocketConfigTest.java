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
    private JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Mock
    private WebSocketHandlerRegistry registry;

    @Mock
    private WebSocketHandlerRegistration registration;

    @Test
    @DisplayName("Should apply configured allowed origins")
    void shouldApplyConfiguredAllowedOrigins() {
        WebSocketConfig config = new WebSocketConfig(
                sismWebSocketHandler,
                jwtHandshakeInterceptor,
                java.util.List.of("http://localhost:3500", "https://sism.example.com")
        );

        doReturn(registration).when(registry).addHandler(sismWebSocketHandler, "/ws/notifications");
        doReturn(registration).when(registration).addInterceptors(jwtHandshakeInterceptor);

        config.registerWebSocketHandlers(registry);

        verify(registration).addInterceptors(jwtHandshakeInterceptor);
        verify(registration).setAllowedOriginPatterns("http://localhost:3500", "https://sism.example.com");
    }
}
