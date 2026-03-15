package com.sism.strategy.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * UpdatePlanRequest - 更新计划请求DTO
 * 用于接收更新计划信息的请求参数
 */
@Data
@Schema(description = "更新计划请求")
public class UpdatePlanRequest {

    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "计划描述")
    private String description;

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
