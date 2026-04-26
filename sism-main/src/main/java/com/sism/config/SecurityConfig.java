package com.sism.config;

import com.sism.iam.application.UserDetailsServiceImpl;
import com.sism.iam.application.JwtTokenService;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Security Configuration
 * 安全配置
 *
 * Provides central security configuration for the application.
 * Configures JWT-based stateless authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    @Value("${app.security.public-docs-enabled:false}")
    private boolean publicDocsEnabled;

    @Value("${app.security.public-actuator-enabled:false}")
    private boolean publicActuatorEnabled;

    /**
     * Password encoder bean for hashing passwords
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JWT Authentication Filter
     * 从请求头中提取JWT token并验证
     */
    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                                        UserDetailsServiceImpl userDetailsService) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        if (jwtTokenService.validateToken(token)) {
                            String username = jwtTokenService.extractUsername(token);
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            token,
                                            userDetails.getAuthorities().isEmpty()
                                                    ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                                    : userDetails.getAuthorities()
                                    );
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    } catch (Exception e) {
                        log.warn("JWT validation failed: {}", e.getMessage());
                    }
                }
                filterChain.doFilter(request, response);
            }

        };
    }

    /**
     * Security filter chain configuration
     * 配置HTTP安全策略：
     * - 公开认证端点（登录/注册/验证）
     * - 公开Swagger文档
     * - 其他端点需要JWT认证
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   OncePerRequestFilter jwtAuthenticationFilter) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless session management (JWT-based)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - Authentication
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register",
                                 "/api/v1/auth/validate", "/api/v1/auth/logout",
                                 "/api/v1/auth/refresh", "/api/v1/auth/health").permitAll()
                // Public endpoints - Health check
                .requestMatchers("/api/v1/actuator/health", "/health", "/error").permitAll()
                // Public endpoints - WebSocket
                .requestMatchers("/ws/**").permitAll()
                // Allow OPTIONS for CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Add JWT filter before Spring Security's UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (publicDocsEnabled) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                            "/api-docs/**", "/v3/api-docs/**").permitAll());
        }

        if (publicActuatorEnabled) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/**", "/api/v1/actuator/**").permitAll());
        }

        return http.build();
    }
}
