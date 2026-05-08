package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkflowTaskStartRequest {

    @NotBlank
    private String workflowId;

    @NotBlank
    private String workflowType;

    @NotBlank
    private String taskName;

    private String taskType;

    private String currentStep;

    private String nextStep;

    private Long initiatorId;

    private Long initiatorOrgId;

    private LocalDateTime dueDate;
}
