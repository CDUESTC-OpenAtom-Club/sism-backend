package com.sism.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for plan approval actions
 */
@Data
public class PlanApprovalRequest {

    @NotNull(message = "Approver ID is required")
    private Long approverId;

    private String comment;
}
