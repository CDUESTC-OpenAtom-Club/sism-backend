package com.sism.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing configuration
 * Automatically populates created_at and updated_at fields
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditConfig {
    
    /**
     * Provides the current auditor (user ID) for audit fields
     * Returns the authenticated user ID from SecurityContext
     */
    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() 
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                // Return empty if no authenticated user (e.g., during system operations)
                return Optional.empty();
            }
            
            // Extract user ID from authentication principal
            // This assumes the principal contains the user ID
            // Adjust based on your authentication implementation
            try {
                Object principal = authentication.getPrincipal();
                if (principal instanceof Long) {
                    return Optional.of((Long) principal);
                } else if (principal instanceof String) {
                    return Optional.of(Long.parseLong((String) principal));
                }
            } catch (Exception e) {
                // Log error and return empty
                return Optional.empty();
            }
            
            return Optional.empty();
        };
    }
}
