package com.sism.vo;

import com.sism.enums.TaskType;

import java.time.LocalDateTime;

/**
 * Value Object for strategic task response
 *
 * Converted to class for compatibility with Java compiler
 * **Validates: Requirements 4.1**
 */
public class TaskVO {
    private Long taskId;
    private Long planId;
    private Long cycleId;
    private String cycleName;
    private Integer year;
    private String taskName;
    private String taskDesc;
    private TaskType taskType;
    private Integer sortOrder;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Approval status computed from associated indicators
     * NOTE: Currently NULL - this is the bug being fixed
     */
    private String approvalStatus;

    /**
     * Default constructor
     */
    public TaskVO() {
    }

    /**
     * Full constructor
     */
    public TaskVO(
        Long taskId,
        Long planId,
        Long cycleId,
        String cycleName,
        Integer year,
        String taskName,
        String taskDesc,
        TaskType taskType,
        Integer sortOrder,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("Task name cannot be null or blank");
        }
        
        this.taskId = taskId;
        this.planId = planId;
        this.cycleId = cycleId;
        this.cycleName = cycleName;
        this.year = year;
        this.taskName = taskName;
        this.taskDesc = taskDesc;
        this.taskType = taskType;
        this.sortOrder = sortOrder;
        this.remark = remark;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getter methods
    public Long getTaskId() { return taskId; }
    public Long getPlanId() { return planId; }
    public Long getCycleId() { return cycleId; }
    public String getCycleName() { return cycleName; }
    public Integer getYear() { return year; }
    public String getTaskName() { return taskName; }
    public String getTaskDesc() { return taskDesc; }
    public TaskType getTaskType() { return taskType; }
    public Integer getSortOrder() { return sortOrder; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getApprovalStatus() { return approvalStatus; }

    // Setter methods
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }
    public void setCycleName(String cycleName) { this.cycleName = cycleName; }
    public void setYear(Integer year) { this.year = year; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public void setTaskDesc(String taskDesc) { this.taskDesc = taskDesc; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setRemark(String remark) { this.remark = remark; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
}