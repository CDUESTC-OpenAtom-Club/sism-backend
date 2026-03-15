package com.sism.strategy.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CreateMilestoneRequest - 创建里程碑请求DTO
 * 用于接收创建里程碑的请求参数
 */
@Data
@Schema(description = "创建里程碑请求")
public class CreateMilestoneRequest {

    @Schema(description = "里程碑名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "里程碑名称不能为空")
    private String milestoneName;

    @Schema(description = "里程碑描述")
    private String description;

    @Schema(description = "目标日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标日期不能为空")
    private LocalDateTime targetDate;

    @Schema(description = "计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @Schema(description = "指标ID")
    private Long indicatorId;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "状态", example = "PLANNED")
    private String status;
}
