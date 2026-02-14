package com.sism.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Value Object for indicator response - Simplified to match database
 */
@Data
public class IndicatorVO {

    private Long indicatorId;
    private Long taskId;
    private Long parentIndicatorId;
    private String indicatorDesc;
    private BigDecimal weightPercent;
    private Integer sortOrder;
    private String remark;
    private String type;
    private Integer progress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 前端需要的关联字段
    private Integer year;  // 从 task -> plan -> cycle 获取
    private String ownerDept;  // 从 task -> plan -> createdByOrg 获取
    private String responsibleDept;  // 从 task -> plan -> targetOrg 获取
    private BigDecimal weight;  // 即 weightPercent
    private String taskName;  // 任务名称（从 strategic_task 获取，如果为空则生成默认名称）
    private Boolean canWithdraw;  // 是否可撤回（用于下发/撤回功能）
    
    // 关联数据（可选）
    private List<IndicatorVO> childIndicators;
    private List<MilestoneVO> milestones;
}
