package com.sism.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Assessment Cycle Value Object
 * Used for API responses
 *
 * Converted to record for immutability and simplicity
 * **Validates: Requirements 4.1**
 *
 * @param cycleId    Cycle ID
 * @param cycleName  Cycle name
 * @param year       Assessment year
 * @param startDate  Start date
 * @param endDate    End date
 * @param description Cycle description
 * @param createdAt  Creation timestamp
 * @param updatedAt  Update timestamp
 */
public record AssessmentCycleVO(
    Long cycleId,
    String cycleName,
    Integer year,
    LocalDate startDate,
    LocalDate endDate,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Compact constructor with validation
     */
    public AssessmentCycleVO {
        if (cycleName == null || cycleName.isBlank()) {
            throw new IllegalArgumentException("Cycle name cannot be null or blank");
        }
        if (year == null) {
            throw new IllegalArgumentException("Year cannot be null");
        }
    }
}
