package com.sism.analytics.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DashboardSummaryDTO - 仪表盘汇总数据
 * Matches frontend DashboardData type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表盘汇总数据")
public class DashboardSummaryDTO {

    @Schema(description = "总得分")
    private double totalScore;

    @Schema(description = "基础指标得分")
    private double basicScore;

    @Schema(description = "发展指标得分")
    private double developmentScore;

    @Schema(description = "完成率")
    private double completionRate;

    @Schema(description = "预警数量")
    private long warningCount;

    @Schema(description = "总指标数")
    private long totalIndicators;

    @Schema(description = "已完成指标数")
    private long completedIndicators;

    @Schema(description = "预警指标分布")
    private AlertIndicators alertIndicators;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "预警指标分布")
    public static class AlertIndicators {
        @Schema(description = "严重预警数")
        private long severe;

        @Schema(description = "中等预警数")
        private long moderate;

        @Schema(description = "一般预警数")
        private long normal;
    }
}
