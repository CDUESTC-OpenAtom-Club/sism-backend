package com.sism.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a milestone
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneCreateRequest {

    @NotNull(message = "Indicator ID is required")
    private Long indicatorId;

    @NotBlank(message = "Milestone name is required")
    @Size(max = 200, message = "Milestone name must not exceed 200 characters")
    private String milestoneName;

    private String milestoneDesc;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private BigDecimal weightPercent = BigDecimal.ZERO;

    private Integer sortOrder = 0;

    private Long inheritedFromId;
}
