package com.sism.dto;

import com.sism.enums.PlanLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a plan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanCreateRequest {

    @NotNull(message = "Cycle ID is required")
    private Long cycleId;

    @NotNull(message = "Target organization ID is required")
    private Long targetOrgId;

    @NotNull(message = "Created by organization ID is required")
    private Long createdByOrgId;

    @NotNull(message = "Plan level is required")
    private PlanLevel planLevel;

    @NotNull(message = "Status is required")
    private String status;
}
