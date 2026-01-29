package com.sism.dto;

import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.ProgressApprovalStatus;
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

    /**
     * 指标状态
     */
    private IndicatorStatus status;

    /**
     * 当前进度（0-100）
     */
    private Integer progress;

    /**
     * 进度审批状态
     */
    private ProgressApprovalStatus progressApprovalStatus;

    /**
     * 待审批进度
     */
    private Integer pendingProgress;

    /**
     * 待审批备注
     */
    private String pendingRemark;

    /**
     * 待审批附件（JSON字符串）
     */
    private String pendingAttachments;

    /**
     * 目标值
     */
    private BigDecimal targetValue;

    /**
     * 实际值
     */
    private BigDecimal actualValue;

    /**
     * 单位
     */
    private String unit;

    /**
     * 负责人
     */
    private String responsiblePerson;
}
