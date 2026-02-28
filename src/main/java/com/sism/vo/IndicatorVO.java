package com.sism.vo;

import com.sism.enums.IndicatorStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Value Object for indicator response
 * Simplified to match database structure
 *
 * Converted to class for compatibility with Java compiler
 * **Validates: Requirements 4.1**
 *
 * Note: Nested collections (childIndicators, milestones) are defensive copies
 * to maintain immutability guarantees
 */
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
    private Integer year;
    private String ownerDept;
    private String responsibleDept;
    private Long targetOrgId;
    private Long ownerOrgId;  // 新增：下发组织ID
    private BigDecimal weight;
    private String taskName;
    private String taskContent;  // 新增：前端使用的字段名（与taskName相同）
    private Boolean canWithdraw;
    private IndicatorStatus status;
    private Boolean isQualitative;
    private String type1;
    private String type2;
    private String level;  // 新增：指标层级
    private String unit;  // 新增：单位
    private BigDecimal actualValue;  // 新增：实际值
    private BigDecimal targetValue;  // 新增：目标值
    private String responsiblePerson;  // 新增：责任人
    private Boolean isStrategic;
    private String statusAudit;  // JSON string for status audit trail
    private String progressApprovalStatus;  // 进度审批状态: NONE, DRAFT, PENDING, APPROVED, REJECTED
    private Integer pendingProgress;  // 待审批进度 (0-100)
    private String pendingRemark;  // 待审批备注
    private String pendingAttachments;  // 待审批附件 (JSON string)
    private List<IndicatorVO> childIndicators;
    private List<MilestoneVO> milestones;

    /**
     * Default constructor
     */
    public IndicatorVO() {
    }

    /**
     * Full constructor
     */
    public IndicatorVO(
        Long indicatorId,
        Long taskId,
        Long parentIndicatorId,
        String indicatorDesc,
        BigDecimal weightPercent,
        Integer sortOrder,
        String remark,
        String type,
        Integer progress,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer year,
        String ownerDept,
        String responsibleDept,
        Long targetOrgId,
        Long ownerOrgId,
        BigDecimal weight,
        String taskName,
        Boolean canWithdraw,
        IndicatorStatus status,
        Boolean isQualitative,
        String type1,
        String type2,
        String level,
        String unit,
        BigDecimal actualValue,
        BigDecimal targetValue,
        String responsiblePerson,
        Boolean isStrategic,
        String statusAudit,
        String progressApprovalStatus,
        Integer pendingProgress,
        String pendingRemark,
        String pendingAttachments,
        List<IndicatorVO> childIndicators,
        List<MilestoneVO> milestones
    ) {
        if (indicatorDesc == null || indicatorDesc.isBlank()) {
            throw new IllegalArgumentException("Indicator description cannot be null or blank");
        }
        
        this.indicatorId = indicatorId;
        this.taskId = taskId;
        this.parentIndicatorId = parentIndicatorId;
        this.indicatorDesc = indicatorDesc;
        this.weightPercent = weightPercent;
        this.sortOrder = sortOrder;
        this.remark = remark;
        this.type = type;
        this.progress = progress;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.year = year;
        this.ownerDept = ownerDept;
        this.responsibleDept = responsibleDept;
        this.targetOrgId = targetOrgId;
        this.ownerOrgId = ownerOrgId;
        this.weight = weight;
        this.taskName = taskName;
        this.taskContent = taskName;  // taskContent 与 taskName 相同
        this.canWithdraw = canWithdraw;
        this.status = status;
        this.isQualitative = isQualitative;
        this.type1 = type1;
        this.type2 = type2;
        this.level = level;
        this.unit = unit;
        this.actualValue = actualValue;
        this.targetValue = targetValue;
        this.responsiblePerson = responsiblePerson;
        this.isStrategic = isStrategic;
        this.statusAudit = statusAudit;
        this.progressApprovalStatus = progressApprovalStatus;
        this.pendingProgress = pendingProgress;
        this.pendingRemark = pendingRemark;
        this.pendingAttachments = pendingAttachments;
        this.childIndicators = childIndicators != null ? List.copyOf(childIndicators) : List.of();
        this.milestones = milestones != null ? List.copyOf(milestones) : List.of();
    }

    /**
     * Canonical constructor for backward compatibility
     * Sets all optional fields to null
     */
    public IndicatorVO(
        Long indicatorId,
        Long taskId,
        Long parentIndicatorId,
        String indicatorDesc,
        BigDecimal weightPercent,
        Integer sortOrder,
        String remark,
        String type,
        Integer progress,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this(
            indicatorId, taskId, parentIndicatorId, indicatorDesc,
            weightPercent, sortOrder, remark, type, progress,
            createdAt, updatedAt, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    // Getter methods
    public Long getIndicatorId() { return indicatorId; }
    public Long getTaskId() { return taskId; }
    public Long getParentIndicatorId() { return parentIndicatorId; }
    public String getIndicatorDesc() { return indicatorDesc; }
    public BigDecimal getWeightPercent() { return weightPercent; }
    public Integer getSortOrder() { return sortOrder; }
    public String getRemark() { return remark; }
    public String getType() { return type; }
    public Integer getProgress() { return progress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Integer getYear() { return year; }
    public String getOwnerDept() { return ownerDept; }
    public String getResponsibleDept() { return responsibleDept; }
    public Long getTargetOrgId() { return targetOrgId; }
    public Long getOwnerOrgId() { return ownerOrgId; }
    public BigDecimal getWeight() { return weight; }
    public String getTaskName() { return taskName; }
    public String getTaskContent() { return taskContent; }
    public Boolean getCanWithdraw() { return canWithdraw; }
    public IndicatorStatus getStatus() { return status; }
    public Boolean getIsQualitative() { return isQualitative; }
    public String getType1() { return type1; }
    public String getType2() { return type2; }
    public String getLevel() { return level; }
    public String getUnit() { return unit; }
    public BigDecimal getActualValue() { return actualValue; }
    public BigDecimal getTargetValue() { return targetValue; }
    public String getResponsiblePerson() { return responsiblePerson; }
    public Boolean getIsStrategic() { return isStrategic; }
    public String getStatusAudit() { return statusAudit; }
    public List<IndicatorVO> getChildIndicators() { return childIndicators; }
    public List<MilestoneVO> getMilestones() { return milestones; }

    // Setter methods
    public void setIndicatorId(Long indicatorId) { this.indicatorId = indicatorId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public void setParentIndicatorId(Long parentIndicatorId) { this.parentIndicatorId = parentIndicatorId; }
    public void setIndicatorDesc(String indicatorDesc) { this.indicatorDesc = indicatorDesc; }
    public void setWeightPercent(BigDecimal weightPercent) { this.weightPercent = weightPercent; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setRemark(String remark) { this.remark = remark; }
    public void setType(String type) { this.type = type; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setYear(Integer year) { this.year = year; }
    public void setOwnerDept(String ownerDept) { this.ownerDept = ownerDept; }
    public void setResponsibleDept(String responsibleDept) { this.responsibleDept = responsibleDept; }
    public void setTargetOrgId(Long targetOrgId) { this.targetOrgId = targetOrgId; }
    public void setOwnerOrgId(Long ownerOrgId) { this.ownerOrgId = ownerOrgId; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public void setTaskName(String taskName) { 
        this.taskName = taskName;
        this.taskContent = taskName;  // 同步更新 taskContent
    }
    public void setTaskContent(String taskContent) { 
        this.taskContent = taskContent;
        this.taskName = taskContent;  // 同步更新 taskName
    }
    public void setCanWithdraw(Boolean canWithdraw) { this.canWithdraw = canWithdraw; }
    public void setStatus(IndicatorStatus status) { this.status = status; }
    public void setIsQualitative(Boolean isQualitative) { this.isQualitative = isQualitative; }
    public void setType1(String type1) { this.type1 = type1; }
    public void setType2(String type2) { this.type2 = type2; }
    public void setLevel(String level) { this.level = level; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setActualValue(BigDecimal actualValue) { this.actualValue = actualValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }
    public void setIsStrategic(Boolean isStrategic) { this.isStrategic = isStrategic; }
    public void setStatusAudit(String statusAudit) { this.statusAudit = statusAudit; }
    public String getProgressApprovalStatus() { return progressApprovalStatus; }
    public void setProgressApprovalStatus(String progressApprovalStatus) { this.progressApprovalStatus = progressApprovalStatus; }
    public Integer getPendingProgress() { return pendingProgress; }
    public void setPendingProgress(Integer pendingProgress) { this.pendingProgress = pendingProgress; }
    public String getPendingRemark() { return pendingRemark; }
    public void setPendingRemark(String pendingRemark) { this.pendingRemark = pendingRemark; }
    public String getPendingAttachments() { return pendingAttachments; }
    public void setPendingAttachments(String pendingAttachments) { this.pendingAttachments = pendingAttachments; }
    public void setChildIndicators(List<IndicatorVO> childIndicators) { 
        this.childIndicators = childIndicators != null ? List.copyOf(childIndicators) : List.of(); 
    }
    public void setMilestones(List<MilestoneVO> milestones) { 
        this.milestones = milestones != null ? List.copyOf(milestones) : List.of(); 
    }
}