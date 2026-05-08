package com.sism.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestLoggingFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldPopulateRequestContextAndCleanUpAfterCompletion() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/reports");
        request.setQueryString("page=1&size=20");
        request.addHeader("X-Request-ID", "req-123");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.addHeader("Authorization", "Bearer token");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> clientIp = new AtomicReference<>();
        AtomicReference<String> requestMethod = new AtomicReference<>();
        AtomicReference<String> requestUri = new AtomicReference<>();
        AtomicReference<String> userId = new AtomicReference<>();

        FilterChain chain = (servletRequest, servletResponse) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            requestId.set(MDC.get("requestId"));
            clientIp.set(MDC.get("clientIp"));
            requestMethod.set(MDC.get("requestMethod"));
            requestUri.set(MDC.get("requestUri"));
            userId.set(MDC.get("userId"));
            httpResponse.setStatus(202);
            httpResponse.getWriter().write("accepted");
        };

        filter.doFilter(request, response, chain);

        assertEquals("req-123", response.getHeader("X-Request-ID"));
        assertEquals("req-123", requestId.get());
        assertEquals("10.0.0.1", clientIp.get());
        assertEquals("POST", requestMethod.get());
        assertEquals("/api/v1/reports?page=1&size=20", requestUri.get());
        assertEquals("pending", userId.get());
        assertEquals(202, response.getStatus());
        assertEquals("accepted", response.getContentAsString());
        assertNull(MDC.get("requestId"));
        assertNull(MDC.get("clientIp"));
        assertNull(MDC.get("requestMethod"));
        assertNull(MDC.get("requestUri"));
        assertNull(MDC.get("responseStatus"));
        assertNull(MDC.get("responseTime"));
    }

    @Test
    void shouldGenerateRequestIdWhenHeaderMissing() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> requestId = new AtomicReference<>();

        FilterChain chain = (servletRequest, servletResponse) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            requestId.set(MDC.get("requestId"));
            httpResponse.setStatus(200);
        };

        filter.doFilter(request, response, chain);

        assertEquals(response.getHeader("X-Request-ID"), requestId.get());
        assertEquals(8, response.getHeader("X-Request-ID").length());
        assertNull(MDC.get("requestId"));
    }

    @Test
    void shouldPreferRealIpWhenForwardedForHeaderIsMissing() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ping");
        request.addHeader("X-Real-IP", "192.168.10.25");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> clientIp = new AtomicReference<>();

        FilterChain chain = (servletRequest, servletResponse) -> {
            clientIp.set(MDC.get("clientIp"));
            ((HttpServletResponse) servletResponse).setStatus(200);
        };

        filter.doFilter(request, response, chain);

        assertEquals("192.168.10.25", clientIp.get());
        assertEquals(200, response.getStatus());
        assertNull(MDC.get("clientIp"));
    }
}
