package com.sism.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for updating a progress report
 * Requirements: 3.1 - Update report (only DRAFT and RETURNED status)
 */
@Data
public class ReportUpdateRequest {

    /**
     * Optional milestone ID - cannot be set together with adhocTaskId
     */
    private Long milestoneId;

    /**
     * Optional adhoc task ID - cannot be set together with milestoneId
     */
    private Long adhocTaskId;

    /**
     * Completion percentage (0-100)
     */
    private BigDecimal percentComplete;

    /**
     * Whether the milestone is achieved
     */
    private Boolean achievedMilestone;

    /**
     * Report narrative/description
     */
    private String narrative;
}
