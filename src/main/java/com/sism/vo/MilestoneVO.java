package com.sism.vo;

import com.sism.enums.MilestoneStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Value Object for milestone response
 *
 * Converted to record for immutability and simplicity
 * **Validates: Requirements 4.1**
 *
 * @param milestoneId     Milestone ID
 * @param indicatorId     Indicator ID
 * @param indicatorDesc   Indicator description
 * @param milestoneName   Milestone name
 * @param milestoneDesc   Milestone description
 * @param dueDate         Due date
 * @param weightPercent   Weight percentage
 * @param status          Milestone status
 * @param sortOrder       Sort order
 * @param inheritedFromId Inherited from ID
 * @param createdAt       Creation timestamp
 * @param updatedAt       Update timestamp
 * @param targetProgress  Target progress percentage (0-100)
 * @param isPaired        Whether paired (has approved fill record)
 */
public record MilestoneVO(
    Long milestoneId,
    Long indicatorId,
    String indicatorDesc,
    String milestoneName,
    String milestoneDesc,
    LocalDate dueDate,
    BigDecimal weightPercent,
    MilestoneStatus status,
    Integer sortOrder,
    Long inheritedFromId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer targetProgress,
    Boolean isPaired
) {
    /**
     * Compact constructor with validation
     */
    public MilestoneVO {
        if (milestoneName == null || milestoneName.isBlank()) {
            throw new IllegalArgumentException("Milestone name cannot be null or blank");
        }
        // Default targetProgress to 0 if null
        if (targetProgress == null) {
            targetProgress = 0;
        }
        // Default isPaired to false if null
        if (isPaired == null) {
            isPaired = false;
        }
    }
}
