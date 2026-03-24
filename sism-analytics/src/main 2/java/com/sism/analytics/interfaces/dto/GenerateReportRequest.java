package com.sism.analytics.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * GenerateReportRequest - 生成报告请求DTO
 * 用于接收生成报告的请求参数
 */
@Data
@Schema(description = "生成报告请求")
public class GenerateReportRequest {

    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件路径不能为空")
    private String filePath;

    @Schema(description = "文件大小(字节)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;
}
