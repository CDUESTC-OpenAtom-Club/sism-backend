package com.sism.analytics.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * CreateDataExportRequest - 创建数据导出请求DTO
 * 用于接收创建数据导出的请求参数
 */
@Data
@Schema(description = "创建数据导出请求")
public class CreateDataExportRequest {

    @Schema(description = "导出任务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "导出任务名称不能为空")
    private String name;

    @Schema(description = "导出类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "INDICATORS")
    @NotBlank(message = "导出类型不能为空")
    private String type;

    @Schema(description = "导出格式", requiredMode = Schema.RequiredMode.REQUIRED, example = "EXCEL")
    @NotBlank(message = "导出格式不能为空")
    private String format;

    @Schema(description = "请求者ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请求者ID不能为空")
    private Long requestedBy;

    @Schema(description = "导出参数(JSON格式)")
    private String parameters;
}
