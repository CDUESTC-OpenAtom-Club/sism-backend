package com.sism.alert.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AlertRequest - 创建预警请求DTO
 */
@Data
@Schema(description = "创建预警请求")
public class AlertRequest {

    @Schema(description = "预警类型（如TASK_OVERDUE, INDICATOR_THRESHOLD等）", requiredMode = Schema.RequiredMode.REQUIRED, example = "TASK_OVERDUE")
    @NotBlank(message = "预警类型不能为空")
    @Size(max = 100, message = "预警类型长度不能超过100个字符")
    private String alertType;

    @Schema(description = "预警标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "任务即将超期预警")
    @NotBlank(message = "预警标题不能为空")
    @Size(max = 200, message = "预警标题长度不能超过200个字符")
    private String title;

    @Schema(description = "预警描述", example = "任务XXX将在3天后到期，请及时处理")
    @Size(max = 1000, message = "预警描述长度不能超过1000个字符")
    private String description;

    @Schema(description = "严重程度（LOW, MEDIUM, HIGH, CRITICAL）", requiredMode = Schema.RequiredMode.REQUIRED, example = "MEDIUM")
    @NotBlank(message = "严重程度不能为空")
    @Size(max = 20, message = "严重程度长度不能超过20个字符")
    private String severity;

    @Schema(description = "关联实体类型（如TASK, INDICATOR等）", example = "TASK")
    @Size(max = 50, message = "实体类型长度不能超过50个字符")
    private String entityType;

    @Schema(description = "关联实体ID", example = "1")
    private Long entityId;
}
