package com.sism.analytics.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * UpdateReportRequest - 更新报告请求DTO
 * 用于接收更新报告的请求参数
 */
@Data
@Schema(description = "更新报告请求")
public class UpdateReportRequest {

    @Schema(description = "报告名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "报告名称不能为空")
    private String name;

    @Schema(description = "报告类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "STRATEGIC")
    @NotBlank(message = "报告类型不能为空")
    private String type;

    @Schema(description = "报告格式", requiredMode = Schema.RequiredMode.REQUIRED, example = "PDF")
    @NotBlank(message = "报告格式不能为空")
    private String format;

    @Schema(description = "报告描述")
    private String description;
}
