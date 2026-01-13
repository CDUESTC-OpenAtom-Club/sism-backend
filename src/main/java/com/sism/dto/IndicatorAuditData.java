package com.sism.dto;

import com.sism.entity.Indicator;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for indicator audit data
 * Used for capturing indicator state in audit logs
 */
@Data
public class IndicatorAuditData {

    private Long indicatorId;
    private Long taskId;
    private Long parentIndicatorId;
    private IndicatorLevel level;
    private Long ownerOrgId;
    private Long targetOrgId;
    private String indicatorDesc;
    private BigDecimal weightPercent;
    private Integer sortOrder;
    private Integer year;
    private IndicatorStatus status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Create audit data from Indicator entity
     */
    public static IndicatorAuditData fromEntity(Indicator indicator) {
        if (indicator == null) {
            return null;
        }
        IndicatorAuditData data = new IndicatorAuditData();
        data.setIndicatorId(indicator.getIndicatorId());
        data.setTaskId(indicator.getTask() != null ? indicator.getTask().getTaskId() : null);
        data.setParentIndicatorId(indicator.getParentIndicator() != null ? 
                indicator.getParentIndicator().getIndicatorId() : null);
        data.setLevel(indicator.getLevel());
        data.setOwnerOrgId(indicator.getOwnerOrg() != null ? indicator.getOwnerOrg().getOrgId() : null);
        data.setTargetOrgId(indicator.getTargetOrg() != null ? indicator.getTargetOrg().getOrgId() : null);
        data.setIndicatorDesc(indicator.getIndicatorDesc());
        data.setWeightPercent(indicator.getWeightPercent());
        data.setSortOrder(indicator.getSortOrder());
        data.setYear(indicator.getYear());
        data.setStatus(indicator.getStatus());
        data.setRemark(indicator.getRemark());
        data.setCreatedAt(indicator.getCreatedAt());
        data.setUpdatedAt(indicator.getUpdatedAt());
        return data;
    }
}
