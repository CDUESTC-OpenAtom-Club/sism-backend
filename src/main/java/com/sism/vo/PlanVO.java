package com.sism.vo;

import com.sism.enums.PlanLevel;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Value Object for plan response
 */
@Data
public class PlanVO {

    private Long id;
    private Long cycleId;
    private Long targetOrgId;
    private Long createdByOrgId;
    private PlanLevel planLevel;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
}
