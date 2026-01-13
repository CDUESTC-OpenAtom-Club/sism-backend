package com.sism.dto;

import com.sism.enums.TaskType;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating a strategic task
 */
@Data
public class TaskUpdateRequest {

    @Size(max = 200, message = "Task name must not exceed 200 characters")
    private String taskName;

    private String taskDesc;

    private TaskType taskType;

    private Long orgId;

    private Integer sortOrder;

    private String remark;
}
