package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Unified task decision request DTO.
 * Uses the current authenticated user as operator.
 */
@Data
public class WorkflowTaskDecisionRequest {

    @NotNull(message = "Decision result is required")
    private Boolean approved;

    private String comment;
}
