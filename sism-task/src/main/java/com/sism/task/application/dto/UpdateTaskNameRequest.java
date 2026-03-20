package com.sism.task.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * UpdateTaskNameRequest - 更新任务名称请求DTO
 */
@Data
public class UpdateTaskNameRequest {

    @NotBlank(message = "任务名称不能为空")
    private String name;
}
