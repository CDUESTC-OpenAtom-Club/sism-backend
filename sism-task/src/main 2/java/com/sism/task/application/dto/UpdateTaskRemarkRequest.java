package com.sism.task.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * UpdateTaskRemarkRequest - 更新任务备注请求DTO
 * 用于专门更新任务备注
 */
@Data
public class UpdateTaskRemarkRequest {

    @NotBlank(message = "任务备注不能为空")
    private String remark;
}
