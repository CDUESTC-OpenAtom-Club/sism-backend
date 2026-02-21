package com.sism.vo;

import com.sism.enums.TaskType;

import java.time.LocalDateTime;

/**
 * Value Object for strategic task response
 *
 * Converted to record for immutability and simplicity
 * **Validates: Requirements 4.1**
 *
 * @param taskId        Task ID
 * @param planId        Plan ID
 * @param cycleId       Cycle ID (保留用于展示)
 * @param cycleName     Cycle name (保留用于展示)
 * @param year          Assessment year
 * @param taskName      Task name
 * @param taskDesc      Task description
 * @param taskType      Task type (enum)
 * @param sortOrder     Sort order
 * @param remark        Remark
 * @param createdAt     Creation timestamp
 * @param updatedAt     Update timestamp
 */
public record TaskVO(
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
    /**
     * Canonical constructor with validation
     */
    public TaskVO {
        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("Task name cannot be null or blank");
        }
    }
}
