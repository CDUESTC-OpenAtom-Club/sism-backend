package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartLegacyInstanceRequest {

    @NotNull
    private Long flowDefId;

    @NotBlank
    private String entityType;

    @NotNull
    private Long entityId;
}
