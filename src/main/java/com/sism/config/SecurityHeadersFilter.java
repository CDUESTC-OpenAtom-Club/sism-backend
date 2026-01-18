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
    
    @Value("${app.security.headers.xss-protection:1; mode=block}")
    private String xssProtection;
    
    @Value("${app.security.headers.referrer-policy:strict-origin-when-cross-origin}")
    private String referrerPolicy;
    
    @Value("${app.security.headers.permissions-policy:geolocation=(), microphone=(), camera=()}")
    private String permissionsPolicy;

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
            
            // X-XSS-Protection: Enables browser's XSS filter
            // 1; mode=block enables filter and blocks page if attack detected
            response.setHeader("X-XSS-Protection", xssProtection);
            
            // Referrer-Policy: Controls how much referrer info is sent
            // strict-origin-when-cross-origin sends full URL for same-origin, only origin for cross-origin
            response.setHeader("Referrer-Policy", referrerPolicy);
            
            // Permissions-Policy: Controls browser features
            // Disables geolocation, microphone, and camera by default
            response.setHeader("Permissions-Policy", permissionsPolicy);
            
            // Cache-Control for API responses
            // Prevents caching of sensitive data
            if (request.getRequestURI().startsWith("/api/")) {
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
