package com.sism.workflow.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作流实例响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceResponse {

    private String instanceId;
    private String definitionId;
    private String status; // External states: IN_REVIEW, APPROVED, REJECTED
    private Long businessEntityId;
    private Long starterId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String currentTaskId;
    private String currentStepName;
    private Long currentApproverId;
    private String currentApproverName;
    private Boolean canWithdraw;
}
