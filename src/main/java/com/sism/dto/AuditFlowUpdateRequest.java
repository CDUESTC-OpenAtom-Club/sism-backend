package com.sism.dto;

import com.sism.enums.AuditEntityType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an audit flow definition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditFlowUpdateRequest {

    @Size(max = 100, message = "Flow name must not exceed 100 characters")
    private String flowName;

    private AuditEntityType entityType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
