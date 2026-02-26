package com.sism.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating an indicator - Simplified to match database
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorCreateRequest {

    @NotNull(message = "Task ID is required")
    private Long taskId;

    private Long parentIndicatorId;

    @NotBlank(message = "Indicator description is required")
    private String indicatorDesc;

    private BigDecimal weightPercent = BigDecimal.ZERO;

    private Integer sortOrder = 0;

    private String remark;

    private String type; // 基础性/发展性

    private Integer progress = 0;
    
    // 新增字段：组织相关
    private Long ownerOrgId;  // 下发部门的组织ID
    private Long targetOrgId;  // 目标部门的组织ID
    private String level;  // STRAT_TO_FUNC 或 FUNC_TO_COLLEGE
    private Integer year;  // 年份
    private Boolean canWithdraw;  // 是否可撤回
}
