package com.sism.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * JPA Auditing configuration
 * Automatically populates created_at and updated_at fields
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@Slf4j
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
            try {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetails userDetails) {
                    String username = userDetails.getUsername();
                    if (username != null && username.matches("\\d+")) {
                        return Optional.of(Long.parseLong(username));
                    }
                } else if (principal instanceof Long) {
                    return Optional.of((Long) principal);
                } else if (principal instanceof String) {
                    String principalValue = (String) principal;
                    if (principalValue.matches("\\d+")) {
                        return Optional.of(Long.parseLong(principalValue));
                    }
                } else if (principal != null) {
                    try {
                        var idMethod = principal.getClass().getMethod("getId");
                        Object idValue = idMethod.invoke(principal);
                        if (idValue instanceof Number number) {
                            return Optional.of(number.longValue());
                        }
                    } catch (NoSuchMethodException ignored) {
                        log.debug("Unsupported principal type for JPA auditing: {}", principal.getClass().getName());
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to resolve auditor from authentication principal", e);
                return Optional.empty();
            }
            
            return Optional.empty();
        };
    }
}
