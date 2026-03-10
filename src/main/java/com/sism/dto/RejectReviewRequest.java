package com.sism.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for indicator review rejection
 * Requirements: 2.8
 */
@Data
public class RejectReviewRequest {

    @NotBlank(message = "Reason is required for rejection")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
