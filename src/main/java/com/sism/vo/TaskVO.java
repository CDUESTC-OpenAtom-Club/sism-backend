package com.sism.vo;

import com.sism.enums.TaskType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Value Object for strategic task response
 */
@Data
public class TaskVO {

    private Long taskId;
    private Long planId;
    private Long cycleId;  // 保留用于展示
    private String cycleName;  // 保留用于展示
    private Integer year;
    private String taskName;
    private String taskDesc;
    private TaskType taskType;
    private Integer sortOrder;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
