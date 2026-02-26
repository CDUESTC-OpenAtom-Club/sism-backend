package com.sism.dto;

import com.sism.enums.MilestoneStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for updating a milestone
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneUpdateRequest {

    @Size(max = 200, message = "Milestone name must not exceed 200 characters")
    private String milestoneName;

    private String milestoneDesc;

    private LocalDate dueDate;

    private BigDecimal weightPercent;

    private MilestoneStatus status;

    private Integer sortOrder;
}
