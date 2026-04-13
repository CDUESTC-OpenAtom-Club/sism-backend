package com.sism.analytics.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * CompleteDataExportRequest - 完成数据导出请求DTO
 * 用于接收完成数据导出的请求参数
 */
@Data
@Schema(description = "完成数据导出请求")
public class CompleteDataExportRequest {

    @Schema(description = "文件大小(字节)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;
}
