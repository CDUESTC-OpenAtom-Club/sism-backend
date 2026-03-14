package com.sism.task.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * UpdateTaskNameRequest - 更新任务名称请求DTO
 */
@Data
public class UpdateTaskNameRequest {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "任务名称不能为空")
    private String taskName;
}
