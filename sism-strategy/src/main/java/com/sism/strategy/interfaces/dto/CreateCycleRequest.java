package com.sism.strategy.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "创建考核周期请求")
@ValidCycleDateRange
public class CreateCycleRequest {

    @Schema(description = "考核周期名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "考核周期名称不能为空")
    private String name;

    @Schema(description = "年份", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    @NotNull(message = "年份不能为空")
    @Min(value = 2000, message = "年份必须大于等于2000")
    @Max(value = 2100, message = "年份必须小于等于2100")
    private Integer year;

    @Schema(description = "开始日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-01")
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "结束日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-12-31")
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
}
