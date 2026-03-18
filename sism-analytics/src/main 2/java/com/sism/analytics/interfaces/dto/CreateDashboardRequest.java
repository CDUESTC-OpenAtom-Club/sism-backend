package com.sism.analytics.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * CreateDashboardRequest - 创建仪表板请求DTO
 * 用于接收创建仪表板的请求参数
 */
@Data
@Schema(description = "创建仪表板请求")
public class CreateDashboardRequest {

    @Schema(description = "仪表板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "仪表板名称不能为空")
    private String name;

    @Schema(description = "仪表板描述")
    private String description;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "是否公开")
    private Boolean isPublic;

    @Schema(description = "仪表板配置(JSON格式)")
    private String config;
}
