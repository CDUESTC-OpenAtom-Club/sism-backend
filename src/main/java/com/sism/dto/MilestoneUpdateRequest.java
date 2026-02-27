package com.sism.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for milestone data in indicator update request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneUpdateRequest {
    
    private Long milestoneId;  // 0 or null means create new milestone
    
    private String milestoneName;
    
    private String milestoneDesc;  // Optional description
    
    private Integer targetProgress;
    
    private String dueDate;  // Format: YYYY-MM-DD (String type for easy parsing)
    
    private String status;  // NOT_STARTED, IN_PROGRESS, COMPLETED, DELAYED, CANCELED (String type for easy parsing)
    
    private BigDecimal weightPercent;
    
    private Integer sortOrder;
}
