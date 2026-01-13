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
}
