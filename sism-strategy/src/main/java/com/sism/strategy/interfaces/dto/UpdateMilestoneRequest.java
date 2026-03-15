package com.sism.strategy.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * UpdateMilestoneRequest - 更新里程碑请求DTO
 * 用于接收更新里程碑信息的请求参数
 */
@Data
@Schema(description = "更新里程碑请求")
public class UpdateMilestoneRequest {

    @Schema(description = "里程碑名称")
    private String milestoneName;

    @Schema(description = "里程碑描述")
    private String description;

    @Schema(description = "目标日期")
    private LocalDateTime targetDate;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "状态", example = "PLANNED")
    private String status;

    @Schema(description = "完成百分比")
    private Integer completionPercentage;
}
