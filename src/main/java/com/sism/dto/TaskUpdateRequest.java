package com.sism.dto;

import com.sism.enums.TaskType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating a strategic task
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {

    @Size(max = 200, message = "Task name must not exceed 200 characters")
    private String taskName;

    private String taskDesc;

    private TaskType taskType;

    private Long orgId;

    private Integer sortOrder;

    private String remark;
}
