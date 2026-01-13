package com.sism.vo;

import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Value Object for indicator response
 */
@Data
public class IndicatorVO {

    private Long indicatorId;
    private Long taskId;
    private String taskName;
    private Long parentIndicatorId;
    private String parentIndicatorDesc;
    private IndicatorLevel level;
    private Long ownerOrgId;
    private String ownerOrgName;
    private Long targetOrgId;
    private String targetOrgName;
    private String indicatorDesc;
    private BigDecimal weightPercent;
    private Integer sortOrder;
    private Integer year;
    private IndicatorStatus status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<IndicatorVO> childIndicators;
    private List<MilestoneVO> milestones;
}
