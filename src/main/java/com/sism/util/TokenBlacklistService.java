package com.sism.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing JWT token blacklist
 * Used to invalidate tokens on logout
 * 
 * Note: In production, consider using Redis for distributed blacklist
 */
@Slf4j
@Service
public class TokenBlacklistService {

    // Token blacklist (in-memory, consider Redis for production)
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    /**
     * Add token to blacklist
     * 
     * @param token JWT token to blacklist
     */
    public void blacklist(String token) {
        if (token != null && !token.isEmpty()) {
            blacklist.add(token);
            log.debug("Token added to blacklist");
        }
    }

    /**
     * Check if token is blacklisted
     * 
     * @param token JWT token to check
     * @return true if token is blacklisted
     */
    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }

    /**
     * Remove token from blacklist (optional cleanup)
     * 
     * @param token JWT token to remove
     */
    public void remove(String token) {
        blacklist.remove(token);
    }

    /**
     * Clear all blacklisted tokens (for testing)
     */
    public void clear() {
        blacklist.clear();
    }
}
