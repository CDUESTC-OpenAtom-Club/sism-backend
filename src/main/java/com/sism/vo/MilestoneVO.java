package com.sism.vo;

import com.sism.enums.MilestoneStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Value Object for milestone response
 *
 * Converted to class for compatibility with Java compiler
 * **Validates: Requirements 4.1**
 */
public class MilestoneVO {
    private Long milestoneId;
    private Long indicatorId;
    private String indicatorDesc;
    private String milestoneName;
    private String milestoneDesc;
    private LocalDate dueDate;
    private BigDecimal weightPercent;
    private MilestoneStatus status;
    private Integer sortOrder;
    private Long inheritedFromId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer targetProgress;
    private Boolean isPaired;

    /**
     * Default constructor
     */
    public MilestoneVO() {
    }

    /**
     * Full constructor with validation
     */
    public MilestoneVO(
        Long milestoneId,
        Long indicatorId,
        String indicatorDesc,
        String milestoneName,
        String milestoneDesc,
        LocalDate dueDate,
        BigDecimal weightPercent,
        MilestoneStatus status,
        Integer sortOrder,
        Long inheritedFromId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer targetProgress,
        Boolean isPaired
    ) {
        if (milestoneName == null || milestoneName.isBlank()) {
            throw new IllegalArgumentException("Milestone name cannot be null or blank");
        }
        
        this.milestoneId = milestoneId;
        this.indicatorId = indicatorId;
        this.indicatorDesc = indicatorDesc;
        this.milestoneName = milestoneName;
        this.milestoneDesc = milestoneDesc;
        this.dueDate = dueDate;
        this.weightPercent = weightPercent;
        this.status = status;
        this.sortOrder = sortOrder;
        this.inheritedFromId = inheritedFromId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.targetProgress = targetProgress != null ? targetProgress : 0;
        this.isPaired = isPaired != null ? isPaired : false;
    }

    // Getter methods
    public Long getMilestoneId() { return milestoneId; }
    public Long getIndicatorId() { return indicatorId; }
    public String getIndicatorDesc() { return indicatorDesc; }
    public String getMilestoneName() { return milestoneName; }
    public String getMilestoneDesc() { return milestoneDesc; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getWeightPercent() { return weightPercent; }
    public MilestoneStatus getStatus() { return status; }
    public Integer getSortOrder() { return sortOrder; }
    public Long getInheritedFromId() { return inheritedFromId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Integer getTargetProgress() { return targetProgress; }
    public Boolean getIsPaired() { return isPaired; }

    // Setter methods
    public void setMilestoneId(Long milestoneId) { this.milestoneId = milestoneId; }
    public void setIndicatorId(Long indicatorId) { this.indicatorId = indicatorId; }
    public void setIndicatorDesc(String indicatorDesc) { this.indicatorDesc = indicatorDesc; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }
    public void setMilestoneDesc(String milestoneDesc) { this.milestoneDesc = milestoneDesc; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setWeightPercent(BigDecimal weightPercent) { this.weightPercent = weightPercent; }
    public void setStatus(MilestoneStatus status) { this.status = status; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setInheritedFromId(Long inheritedFromId) { this.inheritedFromId = inheritedFromId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setTargetProgress(Integer targetProgress) { this.targetProgress = targetProgress; }
    public void setIsPaired(Boolean isPaired) { this.isPaired = isPaired; }
}