package com.sism.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Assessment Cycle Value Object
 * Used for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentCycleVO {

    private Long cycleId;
    
    private String cycleName;
    
    private Integer year;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private String description;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
