package com.sism.analytics.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * CopyDashboardRequest - 复制仪表板请求DTO
 */
@Data
@Schema(description = "复制仪表板请求")
public class CopyDashboardRequest {

    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标用户ID不能为空")
    @Positive(message = "目标用户ID必须为正数")
    private Long targetUserId;
}
