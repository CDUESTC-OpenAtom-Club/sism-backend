package com.sism.analytics.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * CreateReportRequest - 创建报告请求DTO
 * 用于接收创建报告的请求参数
 */
@Data
@Schema(description = "创建报告请求")
public class CreateReportRequest {

    @Schema(description = "报告名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "报告名称不能为空")
    private String name;

    @Schema(description = "报告类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "STRATEGIC")
    @NotBlank(message = "报告类型不能为空")
    private String type;

    @Schema(description = "报告格式", requiredMode = Schema.RequiredMode.REQUIRED, example = "PDF")
    @NotBlank(message = "报告格式不能为空")
    private String format;

    @Schema(description = "生成者ID（前端可传，但服务端会强制使用当前登录用户）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生成者ID不能为空")
    private Long generatedBy;

    @Schema(description = "报告参数(JSON格式)")
    private String parameters;

    @Schema(description = "报告描述")
    private String description;
}
