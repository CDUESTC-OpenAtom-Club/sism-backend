package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateWorkflowStepDefinitionRequest {

    @NotBlank(message = "Step name is required")
    private String stepName;

    @NotNull(message = "Step order is required")
    @Positive(message = "Step order must be positive")
    private Integer stepOrder;

    @NotBlank(message = "Step type is required")
    private String stepType;

    private Long roleId;
}
