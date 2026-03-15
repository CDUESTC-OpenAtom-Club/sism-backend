package com.sism.strategy.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CreatePlanRequest - 创建计划请求DTO
 * 用于接收创建计划的请求参数
 */
@Data
@Schema(description = "创建计划请求")
public class CreatePlanRequest {

    @Schema(description = "计划名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "计划名称不能为空")
    private String planName;

    @Schema(description = "计划描述")
    private String description;

    @Schema(description = "周期ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "周期ID不能为空")
    private Long cycleId;

    @Schema(description = "计划类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "STRATEGY")
    @NotBlank(message = "计划类型不能为空")
    private String planType;

    @Schema(description = "开始日期")
    private LocalDateTime startDate;

    @Schema(description = "结束日期")
    private LocalDateTime endDate;

    @Schema(description = "负责部门")
    private String ownerDepartment;

    @Schema(description = "目标组织ID")
    private Long targetOrgId;

    @Schema(description = "创建组织ID")
    private Long createdByOrgId;
}
