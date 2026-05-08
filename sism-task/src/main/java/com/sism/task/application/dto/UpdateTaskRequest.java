package com.sism.task.application.dto;

import com.sism.task.domain.task.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * UpdateTaskRequest - 更新任务请求DTO
 * 用于全面更新任务的所有属性
 */
@Data
public class UpdateTaskRequest {

    @NotBlank(message = "任务名称不能为空")
    private String name;

    private String desc;

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

    private Integer sortOrder;

    private String remark;
}
