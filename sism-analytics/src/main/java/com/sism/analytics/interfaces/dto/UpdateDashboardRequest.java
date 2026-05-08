package com.sism.analytics.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * UpdateDashboardRequest - 更新仪表板请求DTO
 * 用于接收更新仪表板的请求参数
 */
@Data
@Schema(description = "更新仪表板请求")
public class UpdateDashboardRequest {

    @Schema(description = "仪表板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "仪表板名称不能为空")
    private String name;

    @Schema(description = "仪表板描述")
    private String description;

    @Schema(description = "是否公开")
    private Boolean isPublic;

    @Schema(description = "仪表板配置(JSON格式)")
    @Size(max = 10000, message = "仪表盘配置不能超过10000个字符")
    private String config;
}
