package com.sism.vo;

import com.sism.enums.AuditEntityType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * VO for approval instance details
 */
@Data
public class ApprovalDetailVO {

    private Long id;
    private AuditEntityType entityType;
    private Long entityId;
    private String status;
    private Integer currentStepOrder;
    private String currentStepName;
    private String currentStepDescription;
    
    // Approval context
    private Long submitterDeptId;
    private Long directSupervisorId;
    private Long level2SupervisorId;
    private Long superiorDeptId;
    
    // Approval tracking
    private List<Long> pendingApprovers;
    private List<Long> approvedApprovers;
    private List<Long> rejectedApprovers;
    
    // Timestamps
    private Long initiatedBy;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
}
