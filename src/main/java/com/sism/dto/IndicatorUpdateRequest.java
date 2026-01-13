package com.sism.dto;

import com.sism.enums.IndicatorLevel;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for updating an indicator
 */
@Data
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
}
