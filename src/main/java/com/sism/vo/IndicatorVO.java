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
    private BigDecimal weight;
    private String taskName;
    private Boolean canWithdraw;
    private IndicatorStatus status;
    private Boolean isQualitative;
    private String type1;
    private String type2;
    private Boolean isStrategic;
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
        BigDecimal weight,
        String taskName,
        Boolean canWithdraw,
        IndicatorStatus status,
        Boolean isQualitative,
        String type1,
        String type2,
        Boolean isStrategic,
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
        this.weight = weight;
        this.taskName = taskName;
        this.canWithdraw = canWithdraw;
        this.status = status;
        this.isQualitative = isQualitative;
        this.type1 = type1;
        this.type2 = type2;
        this.isStrategic = isStrategic;
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
            createdAt, updatedAt, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null
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
    public BigDecimal getWeight() { return weight; }
    public String getTaskName() { return taskName; }
    public Boolean getCanWithdraw() { return canWithdraw; }
    public IndicatorStatus getStatus() { return status; }
    public Boolean getIsQualitative() { return isQualitative; }
    public String getType1() { return type1; }
    public String getType2() { return type2; }
    public Boolean getIsStrategic() { return isStrategic; }
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
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public void setCanWithdraw(Boolean canWithdraw) { this.canWithdraw = canWithdraw; }
    public void setStatus(IndicatorStatus status) { this.status = status; }
    public void setIsQualitative(Boolean isQualitative) { this.isQualitative = isQualitative; }
    public void setType1(String type1) { this.type1 = type1; }
    public void setType2(String type2) { this.type2 = type2; }
    public void setIsStrategic(Boolean isStrategic) { this.isStrategic = isStrategic; }
    public void setChildIndicators(List<IndicatorVO> childIndicators) { 
        this.childIndicators = childIndicators != null ? List.copyOf(childIndicators) : List.of(); 
    }
    public void setMilestones(List<MilestoneVO> milestones) { 
        this.milestones = milestones != null ? List.copyOf(milestones) : List.of(); 
    }
}