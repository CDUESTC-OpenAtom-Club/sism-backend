package com.sism.vo;

import com.sism.enums.PlanLevel;

import java.time.LocalDateTime;

/**
 * Value Object for plan response
 *
 * Converted to record for immutability and simplicity
 * **Validates: Requirements 4.1**
 *
 * @param id             Plan ID
 * @param cycleId        Assessment cycle ID
 * @param targetOrgId    Target organization ID
 * @param createdByOrgId Creator organization ID
 * @param planLevel      Plan level (enum)
 * @param status         Plan status
 * @param createdAt      Creation timestamp
 * @param updatedAt      Update timestamp
 * @param isDeleted      Whether deleted
 */
public record PlanVO(
    Long id,
    Long cycleId,
    Long targetOrgId,
    Long createdByOrgId,
    PlanLevel planLevel,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Boolean isDeleted
) {
    /**
     * Compact constructor with validation
     */
    public PlanVO {
        if (planLevel == null) {
            throw new IllegalArgumentException("Plan level cannot be null");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status cannot be null or blank");
        }
        // Default isDeleted to false if null
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
}
