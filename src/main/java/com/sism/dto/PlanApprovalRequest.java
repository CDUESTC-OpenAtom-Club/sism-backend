package com.sism.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for plan approval actions
 * Requirements: 2.4
 * 
 * Note: approverId is extracted from security context, not from request body
 */
@Data
public class PlanApprovalRequest {

    @Size(max = 500, message = "Comment must not exceed 500 characters")
    private String comment;
}
