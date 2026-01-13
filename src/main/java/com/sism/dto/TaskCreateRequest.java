package com.sism.dto;

import com.sism.enums.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for creating a strategic task
 */
@Data
public class TaskCreateRequest {

    @NotNull(message = "Cycle ID is required")
    private Long cycleId;

    @NotBlank(message = "Task name is required")
    @Size(max = 200, message = "Task name must not exceed 200 characters")
    private String taskName;

    private String taskDesc;

    private TaskType taskType = TaskType.BASIC;

    @NotNull(message = "Organization ID is required")
    private Long orgId;

    @NotNull(message = "Created by organization ID is required")
    private Long createdByOrgId;

    private Integer sortOrder = 0;

    private String remark;
}
