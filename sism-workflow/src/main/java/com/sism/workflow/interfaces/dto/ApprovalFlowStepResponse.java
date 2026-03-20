package com.sism.workflow.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlowStepResponse {

    private Long id;
    private String stepName;
    private Integer stepOrder;
    private String stepType;
    private String approverType;
    private Long approverId;
    private Integer timeoutHours;
    private boolean isRequired;
    private Boolean canSkip;
}
