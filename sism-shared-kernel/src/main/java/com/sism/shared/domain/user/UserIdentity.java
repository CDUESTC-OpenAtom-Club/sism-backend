package com.sism.shared.domain.user;

/**
 * Lightweight cross-context user descriptor.
 */
public record UserIdentity(
        Long id,
        String username,
        String realName,
        Long orgId,
        Boolean isActive
) {
}
