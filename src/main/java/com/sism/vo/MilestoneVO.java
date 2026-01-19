package com.sism.vo;

import com.sism.enums.MilestoneStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Value Object for milestone response
 */
@Data
public class MilestoneVO {

    private Long milestoneId;
    private Long indicatorId;
    private String indicatorDesc;
    private String milestoneName;
    private String milestoneDesc;
    private LocalDate dueDate;
    private BigDecimal weightPercent;
    private MilestoneStatus status;
    private Integer sortOrder;
    private Long inheritedFromId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== 新增字段 (前端数据对齐) ====================

    /**
     * 目标进度百分比 (0-100)
     * 对应前端 targetProgress
     */
    private Integer targetProgress;

    /**
     * 是否已配对（有审核通过的填报记录）
     * 对应前端 isPaired
     */
    private Boolean isPaired;
}
