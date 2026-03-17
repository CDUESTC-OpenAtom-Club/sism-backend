package com.sism.alert.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AlertStatsDTO - 预警统计响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预警统计信息")
public class AlertStatsDTO {

    @Schema(description = "未解决的预警总数")
    private long totalOpen;

    @Schema(description = "按严重程度分类的预警数量")
    private Map<String, Long> countBySeverity;
}
