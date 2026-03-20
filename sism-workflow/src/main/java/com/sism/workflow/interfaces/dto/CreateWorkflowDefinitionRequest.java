package com.sism.workflow.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateWorkflowDefinitionRequest {

    @NotBlank(message = "Definition code is required")
    private String definitionCode;

    @NotBlank(message = "Definition name is required")
    private String definitionName;

    private String description;

    @NotBlank(message = "Entity type is required")
    private String category;

    private boolean isActive = true;

    private Integer version;

    @Valid
    @NotEmpty(message = "Workflow steps are required")
    private List<CreateWorkflowStepDefinitionRequest> steps;
}
