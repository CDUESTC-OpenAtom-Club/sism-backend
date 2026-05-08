package com.sism.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to add security headers to all HTTP responses.
 * These headers help protect against common web vulnerabilities.
 * 
 * Requirements: 7.5 - Configure security headers
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Value("${app.security.headers.enabled:true}")
    private boolean headersEnabled;

    @Value("${app.security.headers.frame-options:SAMEORIGIN}")
    private String frameOptions;

    @Value("${app.security.headers.content-type-options:nosniff}")
    private String contentTypeOptions;

    @Value("${app.security.headers.content-security-policy:default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'}")
    private String contentSecurityPolicy;

    @Value("${app.security.headers.strict-transport-security:max-age=31536000; includeSubDomains}")
    private String strictTransportSecurity;

    @Value("${app.security.headers.referrer-policy:strict-origin-when-cross-origin}")
    private String referrerPolicy;

    @Value("${app.security.headers.permissions-policy:geolocation=(), microphone=(), camera=()}")
    private String permissionsPolicy;

    @Value("${app.security.headers.api-path-prefixes:/api/}")
    private String apiPathPrefixes;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (headersEnabled) {
            // X-Frame-Options: Prevents clickjacking attacks
            // SAMEORIGIN allows framing only from same origin
            response.setHeader("X-Frame-Options", frameOptions);

            // X-Content-Type-Options: Prevents MIME type sniffing
            // nosniff tells browser to strictly follow Content-Type header
            response.setHeader("X-Content-Type-Options", contentTypeOptions);

            // Content-Security-Policy: Restricts where the browser can load assets from.
            response.setHeader("Content-Security-Policy", contentSecurityPolicy);

            // Strict-Transport-Security: Enforce HTTPS only when the request is already secure.
            if (isSecureRequest(request)) {
                response.setHeader("Strict-Transport-Security", strictTransportSecurity);
            }

            // Referrer-Policy: Controls how much referrer info is sent
            // strict-origin-when-cross-origin sends full URL for same-origin, only origin for cross-origin
            response.setHeader("Referrer-Policy", referrerPolicy);

            // Permissions-Policy: Controls browser features
            // Disables geolocation, microphone, and camera by default
            response.setHeader("Permissions-Policy", permissionsPolicy);

            // Cache-Control for API responses
            // Prevents caching of sensitive data
            if (shouldApplyNoCacheHeaders(request.getRequestURI())) {
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldApplyNoCacheHeaders(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return false;
        }

        String[] prefixes = apiPathPrefixes == null ? new String[0] : apiPathPrefixes.split(",");
        for (String prefix : prefixes) {
            String trimmedPrefix = prefix.trim();
            if (!trimmedPrefix.isEmpty() && requestUri.startsWith(trimmedPrefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && forwardedProto.equalsIgnoreCase("https")) {
            return true;
        }

        String forwarded = request.getHeader("Forwarded");
        return forwarded != null && forwarded.toLowerCase().contains("proto=https");
    }
}
