package com.sism.strategy.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MilestoneResponse - 里程碑响应DTO
 * 用于返回里程碑的完整信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "里程碑响应")
public class MilestoneResponse {

    @Schema(description = "里程碑ID")
    private Long id;

    @Schema(description = "里程碑名称")
    private String milestoneName;

    @Schema(description = "里程碑描述")
    private String description;

    @Schema(description = "目标日期")
    private LocalDateTime targetDate;

    @Schema(description = "实际完成日期")
    private LocalDateTime actualDate;

    @Schema(description = "状态", example = "PLANNED")
    private String status;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "完成百分比")
    private Integer completionPercentage;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "指标ID")
    private Long indicatorId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "进度")
    private Integer progress;
}
