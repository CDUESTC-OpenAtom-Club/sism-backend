package com.sism.dto;

import com.sism.enums.IndicatorLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating an indicator
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorUpdateRequest {

    private Long parentIndicatorId;

    private IndicatorLevel level;

    private Long ownerOrgId;

    private Long targetOrgId;

    private String indicatorDesc;

    private BigDecimal weightPercent;

    private Integer sortOrder;

    private Integer year;

    private String remark;

    /**
     * 是否可撤回（用于指标下发/撤回状态控制）
     */
    private Boolean canWithdraw;
}
