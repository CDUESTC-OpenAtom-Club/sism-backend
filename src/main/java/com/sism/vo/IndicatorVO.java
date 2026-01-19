package com.sism.vo;

import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.ProgressApprovalStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Value Object for indicator response
 * 
 * Updated: 2026-01-19 - Added fields for frontend data alignment
 * Requirements: data-alignment-sop 5.1, 5.2
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

    // ==================== 新增字段 (前端数据对齐) ====================

    /**
     * 是否为定性指标
     * 对应前端 isQualitative
     */
    private Boolean isQualitative;

    /**
     * 指标类型1: 定性/定量
     * 对应前端 type1
     */
    private String type1;

    /**
     * 指标类型2: 发展性/基础性
     * 对应前端 type2
     */
    private String type2;

    /**
     * 是否可撤回
     * 对应前端 canWithdraw
     */
    private Boolean canWithdraw;

    /**
     * 目标值
     * 对应前端 targetValue
     */
    private BigDecimal targetValue;

    /**
     * 实际值
     * 对应前端 actualValue
     */
    private BigDecimal actualValue;

    /**
     * 单位
     * 对应前端 unit
     */
    private String unit;

    /**
     * 责任人姓名
     * 对应前端 responsiblePerson
     */
    private String responsiblePerson;

    /**
     * 当前进度百分比 (0-100)
     * 对应前端 progress
     */
    private Integer progress;

    /**
     * 状态审计日志 (JSON字符串)
     * 对应前端 statusAudit
     */
    private String statusAudit;

    /**
     * 进度审批状态
     * 对应前端 progressApprovalStatus
     */
    private ProgressApprovalStatus progressApprovalStatus;

    /**
     * 待审批的进度值
     * 对应前端 pendingProgress
     */
    private Integer pendingProgress;

    /**
     * 待审批的说明
     * 对应前端 pendingRemark
     */
    private String pendingRemark;

    /**
     * 待审批的附件URL列表 (JSON字符串)
     * 对应前端 pendingAttachments
     */
    private String pendingAttachments;

    /**
     * 是否为战略级指标 (派生字段)
     * 对应前端 isStrategic
     * 根据 level 计算: STRAT_TO_FUNC -> true, FUNC_TO_COLLEGE -> false
     */
    private Boolean isStrategic;

    /**
     * 责任部门名称 (派生字段)
     * 对应前端 responsibleDept
     * 等同于 targetOrgName
     */
    private String responsibleDept;

    /**
     * 发布方部门名称 (派生字段)
     * 对应前端 ownerDept
     * 等同于 ownerOrgName
     */
    private String ownerDept;
}
