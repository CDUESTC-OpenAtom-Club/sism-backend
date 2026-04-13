package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkflowTaskFailRequest {

    @NotBlank
    private String errorMessage;
}
