package com.sism.vo;

import com.sism.enums.AuditEntityType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Value Object for audit flow definition response
 */
@Data
public class AuditFlowVO {

    private Long id;
    private String flowName;
    private String flowCode;
    private AuditEntityType entityType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AuditStepVO> steps;
}
