package com.sism.analytics.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DepartmentProgressDTO - 部门进度数据
 * Matches frontend DepartmentProgress type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "部门进度数据")
public class DepartmentProgressDTO {

    @Schema(description = "部门名称")
    private String dept;

    @Schema(description = "进度百分比")
    private double progress;

    @Schema(description = "得分")
    private double score;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "总指标数")
    private long totalIndicators;

    @Schema(description = "已完成指标数")
    private long completedIndicators;

    @Schema(description = "预警数量")
    private long alertCount;
}
