package com.sism.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration
 * Disables authentication for integration tests
 * 
 * This configuration is automatically picked up by Spring Boot tests
 * when the "test" profile is active.
 */
@TestConfiguration
@EnableWebSecurity
@Profile("test")
@Order(1)
public class TestSecurityConfig {

    /**
     * Security filter chain for tests - permits all requests
     * This overrides the production SecurityConfig for test profile
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for tests
            .csrf(AbstractHttpConfigurer::disable)
            // Disable CORS for tests
            .cors(AbstractHttpConfigurer::disable)
            // Disable frame options for H2 console if needed
            .headers(headers -> headers.frameOptions().disable())
            // Permit all requests in test environment
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        
        return http.build();
    }
}
