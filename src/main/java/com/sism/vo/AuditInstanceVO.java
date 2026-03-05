package com.sism.vo;

import com.sism.enums.AuditEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Value Object for AuditInstance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditInstanceVO {

    private Long id;
    private Long flowId;
    private String flowName;
    private Long entityId;
    private AuditEntityType entityType;
    private String status;
    private Integer currentStepOrder;
    private String currentStepName;
    private Long initiatedBy;
    private String initiatorName;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
    
    // Approval chain information
    private Long submitterDeptId;
    private String submitterDeptName;
    private Long directSupervisorId;
    private String directSupervisorName;
    private Long level2SupervisorId;
    private String level2SupervisorName;
    private Long superiorDeptId;
    private String superiorDeptName;
    
    // Approval tracking
    private List<Long> pendingApprovers;
    private List<Long> approvedApprovers;
    private List<Long> rejectedApprovers;
}
