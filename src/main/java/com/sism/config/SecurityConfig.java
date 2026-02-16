package com.sism.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the application
 * Configures JWT authentication filter chain and security settings
 * 
 * Requirements: 1.2
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Password encoder bean using BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security filter chain configuration
     * Configures public paths, JWT filter, and CSRF settings
     * 
     * Requirements: 1.2 - Configure JWT filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST API (frontend-backend separation)
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless session management (JWT-based)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                // Note: context path is /api, so actual path is /auth/** not /api/auth/**
                .requestMatchers("/auth/**").permitAll()
                // Health check endpoint (frontend monitoring)
                .requestMatchers("/health").permitAll()
                .requestMatchers("/health/**").permitAll()
                // Organization endpoints (needed for dashboard initialization)
                .requestMatchers("/organizations/**").permitAll()
                // Swagger/OpenAPI documentation - multiple patterns for compatibility
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/v3/api-docs.yaml").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                // Actuator endpoints
                .requestMatchers("/actuator/**").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Add JWT authentication filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
