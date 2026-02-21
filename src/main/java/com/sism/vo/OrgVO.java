package com.sism.vo;

import com.sism.enums.OrgType;

/**
 * Value Object for organization data
 * Used for API responses
 * Flat structure without parent-child relationships
 *
 * Converted to record for immutability and simplicity
 * **Validates: Requirements 4.1**
 *
 * @param orgId     Organization ID
 * @param orgName   Organization name
 * @param orgType   Organization type (enum)
 * @param isActive  Whether the organization is active
 * @param sortOrder Sort order for display
 */
public record OrgVO(
    Long orgId,
    String orgName,
    OrgType orgType,
    Boolean isActive,
    Integer sortOrder
) {
    /**
     * Canonical constructor with validation
     */
    public OrgVO {
        if (orgName == null || orgName.isBlank()) {
            throw new IllegalArgumentException("Organization name cannot be null or blank");
        }
        if (orgType == null) {
            throw new IllegalArgumentException("Organization type cannot be null");
        }
        // Default isActive to true if null
        if (isActive == null) {
            isActive = true;
        }
        // Default sortOrder to 0 if null
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
