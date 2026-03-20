package com.sism.task.application.dto;

import com.sism.task.domain.TaskCategory;
import com.sism.task.domain.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * CreateTaskRequest - 创建任务请求DTO
 */
@Data
public class CreateTaskRequest {

    private TaskCategory taskCategory = TaskCategory.STRATEGIC;

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @NotNull(message = "任务类型不能为空")
    private TaskType taskType;

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "考核周期ID不能为空")
    private Long cycleId;

    @NotNull(message = "组织ID不能为空")
    private Long orgId;

    @NotNull(message = "创建组织ID不能为空")
    private Long createdByOrgId;

    private Integer sortOrder = 0;
    private String taskDesc;
    private String remark;
}
