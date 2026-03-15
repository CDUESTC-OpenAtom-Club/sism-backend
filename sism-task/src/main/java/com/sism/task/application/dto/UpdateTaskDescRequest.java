package com.sism.task.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * UpdateTaskDescRequest - 更新任务描述请求DTO
 * 用于专门更新任务描述
 */
@Data
public class UpdateTaskDescRequest {

    @NotBlank(message = "任务描述不能为空")
    private String taskDesc;
}
