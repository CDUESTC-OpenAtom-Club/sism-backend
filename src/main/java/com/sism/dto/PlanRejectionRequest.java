package com.sism.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for plan rejection actions
 * Requirements: 2.4
 * 
 * Note: approverId is extracted from security context, not from request body
 */
@Data
public class PlanRejectionRequest {

    @NotBlank(message = "Reason is required for rejection")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
