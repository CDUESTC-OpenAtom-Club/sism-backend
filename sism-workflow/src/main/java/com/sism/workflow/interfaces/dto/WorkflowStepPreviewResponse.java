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
public class WorkflowStepPreviewResponse {
    private Long stepDefId;
    private Integer stepOrder;
    private String stepName;
    private String stepType;
    private Long roleId;
    private boolean selectable;
    private List<ApproverCandidateResponse> candidateApprovers;
}
