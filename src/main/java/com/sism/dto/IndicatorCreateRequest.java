package com.sism.dto;

import com.sism.enums.IndicatorLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating an indicator
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorCreateRequest {

    @NotNull(message = "Task ID is required")
    private Long taskId;

    private Long parentIndicatorId;

    @NotNull(message = "Indicator level is required")
    private IndicatorLevel level;

    @NotNull(message = "Owner organization ID is required")
    private Long ownerOrgId;

    @NotNull(message = "Target organization ID is required")
    private Long targetOrgId;

    @NotBlank(message = "Indicator description is required")
    private String indicatorDesc;

    private BigDecimal weightPercent = BigDecimal.ZERO;

    private Integer sortOrder = 0;

    @NotNull(message = "Year is required")
    private Integer year;

    private String remark;

    /**
     * 是否可撤回（用于指标下发/撤回状态控制）
     */
    private Boolean canWithdraw;
}
