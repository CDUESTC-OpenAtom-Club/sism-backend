package com.sism.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

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
     * CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:3500",
            "http://localhost:3000",
            "http://localhost:5173"
        ));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
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
            // Enable CORS with custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Stateless session management (JWT-based)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // IMPORTANT: Allow OPTIONS requests for CORS preflight FIRST
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public endpoints - no authentication required (with /api prefix)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                // Health check endpoint
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/health/**").permitAll()
                // Organization endpoints
                .requestMatchers("/api/orgs/**").permitAll()
                .requestMatchers("/api/organizations/**").permitAll()
                .requestMatchers("/orgs/**").permitAll()
                .requestMatchers("/organizations/**").permitAll()
                // Dashboard endpoints
                .requestMatchers("/api/dashboard/**").permitAll()
                .requestMatchers("/dashboard/**").permitAll()
                // Indicator endpoints
                .requestMatchers("/api/indicators/**").permitAll()
                .requestMatchers("/indicators/**").permitAll()
                // Task endpoints
                .requestMatchers("/api/tasks/**").permitAll()
                .requestMatchers("/api/strategic/**").permitAll()
                .requestMatchers("/tasks/**").permitAll()
                .requestMatchers("/strategic/**").permitAll()
                // Milestone endpoints
                .requestMatchers("/api/milestones/**").permitAll()
                .requestMatchers("/milestones/**").permitAll()
                // Plan endpoints
                .requestMatchers("/api/plans/**").permitAll()
                .requestMatchers("/plans/**").permitAll()
                // User endpoints
                .requestMatchers("/api/users/**").permitAll()
                .requestMatchers("/users/**").permitAll()
                // Admin endpoints - require authentication
                .requestMatchers("/api/admin/**").authenticated()
                .requestMatchers("/admin/**").authenticated()
                // Swagger/OpenAPI documentation
                .requestMatchers("/api/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/api/v3/api-docs/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/v3/api-docs.yaml").permitAll()
                .requestMatchers("/api/swagger-resources/**").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/api/webjars/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                // Actuator endpoints
                .requestMatchers("/api/actuator/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Add JWT authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
