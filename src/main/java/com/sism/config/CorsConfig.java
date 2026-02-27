package com.sism.config;

import org.springframework.beans.factory.annotation.Value;
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
 * NOTE: 此配置已禁用，改用 SecurityConfig 中的 CORS 配置
 * 避免多个 CORS 过滤器冲突导致 "Invalid CORS request" 错误
 * 
 * Requirements: 7.3 - Configure CORS for production domain
 */
@Configuration
public class CorsConfig {
    
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:3500}")
    private String allowedOrigins;
    
    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;
    
    @Value("${app.cors.allowed-headers:Authorization,Content-Type,Accept,X-Requested-With,X-Timestamp,X-Signature}")
    private String allowedHeaders;
    
    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;
    
    @Value("${app.cors.max-age:3600}")
    private long maxAge;
    
    // 禁用独立的 CorsFilter，避免与 SecurityConfig 中的 CORS 配置冲突
    // 多个 CORS 过滤器会导致 "Invalid CORS request" 403 错误
    //
    // @Bean
    // public CorsFilter corsFilter() {
    //     CorsConfiguration config = new CorsConfiguration();
    //     
    //     config.setAllowCredentials(allowCredentials);
    //     
    //     List<String> origins = Arrays.asList(allowedOrigins.split(","));
    //     
    //     for (String origin : origins) {
    //         String trimmedOrigin = origin.trim();
    //         if (trimmedOrigin.contains("*")) {
    //             config.addAllowedOriginPattern(trimmedOrigin);
    //         } else {
    //             config.addAllowedOrigin(trimmedOrigin);
    //         }
    //     }
    //     
    //     config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
    //     
    //     config.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
    //     
    //     config.setExposedHeaders(Arrays.asList(
    //         "Authorization",
    //         "X-Request-ID",
    //         "Content-Disposition"
    //     ));
    //     
    //     config.setMaxAge(maxAge);
    //     
    //     UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    //     source.registerCorsConfiguration("/**", config);
    //     
    //     return new CorsFilter(source);
    // }
}
