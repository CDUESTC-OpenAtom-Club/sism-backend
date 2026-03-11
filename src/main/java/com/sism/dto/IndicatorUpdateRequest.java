package com.sism.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for updating an indicator - Simplified to match database
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorUpdateRequest {

    private Long parentIndicatorId;

    private String indicatorDesc;

    private BigDecimal weightPercent;

    private Integer sortOrder;

    private String remark;

    private String type;

    private Integer progress;
    
    private Boolean canWithdraw;
    
    private String status;  // Lifecycle status (DRAFT, PENDING, DISTRIBUTED, ACTIVE, ARCHIVED)
    
    private List<MilestoneUpdateRequest> milestones;
    
    private String statusAudit;  // JSON string for status audit trail
    
    private String progressApprovalStatus;  // 进度审批状态
    
    private Integer pendingProgress;  // 待审批进度
    
    private String pendingRemark;  // 待审批备注
    
    private String pendingAttachments;  // 待审批附件 (JSON string)
}
