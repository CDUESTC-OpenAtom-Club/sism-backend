package com.sism.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating an audit step definition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditStepCreateRequest {

    @NotNull(message = "Flow ID is required")
    private Long flowId;

    @NotNull(message = "Step order is required")
    private Integer stepOrder;

    @NotBlank(message = "Step name is required")
    @Size(max = 100, message = "Step name must not exceed 100 characters")
    private String stepName;

    @NotNull(message = "Approver role ID is required")
    private Long approverRoleId;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
