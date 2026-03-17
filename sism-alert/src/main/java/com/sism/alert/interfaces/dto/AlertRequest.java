package com.sism.alert.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AlertRequest - 创建预警请求DTO
 */
@Data
@Schema(description = "创建预警请求")
public class AlertRequest {

    @Schema(description = "关联指标ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "指标ID不能为空")
    private Long indicatorId;

    @Schema(description = "预警规则ID", example = "1")
    private Long ruleId;

    @Schema(description = "预警窗口ID", example = "1")
    private Long windowId;

    @Schema(description = "严重程度（CRITICAL, MAJOR, MINOR）", requiredMode = Schema.RequiredMode.REQUIRED, example = "MAJOR")
    @NotBlank(message = "严重程度不能为空")
    private String severity;

    @Schema(description = "实际完成百分比", example = "45.50")
    private BigDecimal actualPercent;

    @Schema(description = "预期完成百分比", example = "80.00")
    private BigDecimal expectedPercent;

    @Schema(description = "差距百分比", example = "-34.50")
    private BigDecimal gapPercent;

    @Schema(description = "详情JSON", example = "{}")
    private String detailJson;
}
