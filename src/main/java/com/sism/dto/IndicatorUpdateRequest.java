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
    
    private List<MilestoneUpdateRequest> milestones;
    
    private String statusAudit;  // JSON string for status audit trail
}
