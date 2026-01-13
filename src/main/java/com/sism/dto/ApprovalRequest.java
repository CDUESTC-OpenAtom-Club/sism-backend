package com.sism.dto;

import com.sism.enums.ApprovalAction;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for approval action request
 * Requirements: 4.1, 4.2, 4.3, 4.4
 */
@Data
public class ApprovalRequest {

    @NotNull(message = "Report ID is required")
    private Long reportId;

    @NotNull(message = "Approver ID is required")
    private Long approverId;

    @NotNull(message = "Approval action is required")
    private ApprovalAction action;

    /**
     * Comment for the approval action (required for REJECT and RETURN)
     */
    private String comment;
}
