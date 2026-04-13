package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLegacyFlowStepRequest {

    @NotNull
    private Integer stepOrder;

    @NotBlank
    private String stepName;

    private String stepType;

    private Long roleId;

    private Boolean isRequired = Boolean.TRUE;

    private Boolean isTerminal = Boolean.FALSE;
}
