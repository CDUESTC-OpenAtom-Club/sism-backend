package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkflowTaskCompleteRequest {

    @NotBlank
    private String result;
}
