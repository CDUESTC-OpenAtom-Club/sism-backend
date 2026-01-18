package com.sism.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) configuration
 * Allows frontend applications to access backend APIs
 * 
 * Requirements: 7.3 - Configure CORS for production domain
 */
@Configuration
public class CorsConfig {
    
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;
    
    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;
    
    @Value("${app.cors.allowed-headers:Authorization,Content-Type,Accept,X-Requested-With,X-Timestamp,X-Signature}")
    private String allowedHeaders;
    
    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;
    
    @Value("${app.cors.max-age:3600}")
    private long maxAge;
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(allowCredentials);
        
        // Parse allowed origins from configuration
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        
        // Use allowedOriginPatterns for wildcard support
        for (String origin : origins) {
            String trimmedOrigin = origin.trim();
            if (trimmedOrigin.contains("*")) {
                // Use pattern for wildcard origins
                config.addAllowedOriginPattern(trimmedOrigin);
            } else {
                // Use exact origin for non-wildcard
                config.addAllowedOrigin(trimmedOrigin);
            }
        }
        
        // Allowed HTTP methods
        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        
        // Allowed headers
        config.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        
        // Exposed headers (accessible to frontend)
        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Request-ID",
            "Content-Disposition"
        ));
        
        // Max age for preflight requests (in seconds)
        config.setMaxAge(maxAge);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
