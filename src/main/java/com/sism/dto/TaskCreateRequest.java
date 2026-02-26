package com.sism.dto;

import com.sism.enums.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a strategic task
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotBlank(message = "Task name is required")
    @Size(max = 200, message = "Task name must not exceed 200 characters")
    private String taskName;

    private String taskDesc;

    private TaskType taskType = TaskType.BASIC;

    private Integer sortOrder = 0;

    private String remark;
}
