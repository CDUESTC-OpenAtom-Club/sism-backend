package com.sism.vo;

import com.sism.enums.OrgType;

/**
 * Value Object for user data
 * Used for API responses, includes organization information
 *
 * Converted to record for immutability and simplicity
 * **Validates: Requirements 4.1**
 *
 * @param userId   User ID
 * @param username Username
 * @param realName Real name
 * @param isActive Whether the user is active
 * @param orgId    Organization ID
 * @param orgName  Organization name
 * @param orgType  Organization type
 */
public record UserVO(
    Long userId,
    String username,
    String realName,
    Boolean isActive,
    Long orgId,
    String orgName,
    OrgType orgType
) {
    /**
     * Compact constructor with validation
     */
    public UserVO {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        // Default isActive to true if null
        if (isActive == null) {
            isActive = true;
        }
    }
}
