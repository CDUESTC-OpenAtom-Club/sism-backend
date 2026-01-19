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
    private Long cycleId;
    private String cycleName;
    private Integer year;  // 年份，从关联的 AssessmentCycle 获取
    private String taskName;
    private String taskDesc;
    private TaskType taskType;
    private Long orgId;
    private String orgName;
    private Long createdByOrgId;
    private String createdByOrgName;
    private Integer sortOrder;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
