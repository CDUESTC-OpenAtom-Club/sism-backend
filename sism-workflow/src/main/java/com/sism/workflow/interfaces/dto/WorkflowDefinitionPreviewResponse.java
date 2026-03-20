package com.sism.workflow.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinitionPreviewResponse {
    private String workflowCode;
    private String workflowName;
    private String entityType;
    private List<WorkflowStepPreviewResponse> steps;
}
