package com.sism.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Value Object for audit step definition response
 */
@Data
public class AuditStepVO {

    private Long id;
    private Long flowId;
    private Integer stepOrder;
    private String stepName;
    private Long approverRoleId;
    private String approverRoleName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
