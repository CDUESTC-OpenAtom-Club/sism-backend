package com.sism.config;

import com.sism.iam.application.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtHandshakeInterceptor Tests")
class JwtHandshakeInterceptorTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private WebSocketHandler webSocketHandler;

    @Test
    @DisplayName("Should reject handshake without valid token")
    void shouldRejectHandshakeWithoutValidToken() {
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(jwtTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(response),
                webSocketHandler,
                new HashMap<>()
        );

        assertFalse(allowed);
        assertTrue(response.getStatus() == HttpStatus.UNAUTHORIZED.value() || response.getStatus() == 0);
    }

    @Test
    @DisplayName("Should allow handshake with valid token")
    void shouldAllowHandshakeWithValidToken() {
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(jwtTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws/notifications");
        request.setParameter("token", "valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenService.validateToken("valid-token")).thenReturn(true);

        boolean allowed = interceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(response),
                webSocketHandler,
                new HashMap<>()
        );

        assertTrue(allowed);
    }
}
