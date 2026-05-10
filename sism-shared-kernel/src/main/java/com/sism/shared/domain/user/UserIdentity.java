package com.sism.shared.domain.user;

/**
 * Lightweight cross-context user descriptor.
 */
public record UserIdentity(
        Long id,
        String username,
        String realName,
        Long orgId,
        Boolean isActive,
        Boolean isDemo
) {
    /**
     * Backward-compatible 5-arg constructor (isDemo defaults to false).
     */
    public UserIdentity(Long id, String username, String realName, Long orgId, Boolean isActive) {
        this(id, username, realName, orgId, isActive, false);
    }
}
