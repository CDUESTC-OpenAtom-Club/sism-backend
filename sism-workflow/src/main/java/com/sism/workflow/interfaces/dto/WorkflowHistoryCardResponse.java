package com.sism.workflow.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowHistoryCardResponse {

    private String instanceId;
    private String instanceNo;
    private Integer roundNo;
    private String entityType;
    private Long entityId;
    private Long planId;
    private String planName;
    private String flowCode;
    private String flowName;
    private Long sourceOrgId;
    private String sourceOrgName;
    private Long targetOrgId;
    private String targetOrgName;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long requesterId;
    private String requesterName;
}
