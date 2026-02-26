package com.sism.dto;

import com.sism.enums.AuditEntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating an audit flow definition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditFlowCreateRequest {

    @NotBlank(message = "Flow name is required")
    @Size(max = 100, message = "Flow name must not exceed 100 characters")
    private String flowName;

    @NotBlank(message = "Flow code is required")
    @Size(max = 50, message = "Flow code must not exceed 50 characters")
    private String flowCode;

    @NotNull(message = "Entity type is required")
    private AuditEntityType entityType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
