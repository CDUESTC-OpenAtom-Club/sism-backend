package com.sism.workflow.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateLegacyFlowRequest {

    @NotBlank
    private String flowCode;

    @NotBlank
    private String flowName;

    private String description;

    @NotBlank
    private String entityType;

    private Boolean isActive = Boolean.TRUE;

    private Integer version = 1;

    @Valid
    @NotEmpty
    private List<CreateLegacyFlowStepRequest> steps;
}
