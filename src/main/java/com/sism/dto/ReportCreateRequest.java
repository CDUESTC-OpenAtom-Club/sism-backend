package com.sism.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for creating a progress report
 * Requirements: 3.1 - Create progress report in draft status
 */
@Data
public class ReportCreateRequest {

    @NotNull(message = "Indicator ID is required")
    private Long indicatorId;

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
    private BigDecimal percentComplete = BigDecimal.ZERO;

    /**
     * Whether the milestone is achieved
     */
    private Boolean achievedMilestone = false;

    /**
     * Report narrative/description
     */
    private String narrative;

    @NotNull(message = "Reporter ID is required")
    private Long reporterId;
}
