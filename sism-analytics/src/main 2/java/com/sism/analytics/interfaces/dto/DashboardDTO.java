package com.sism.analytics.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DashboardDTO - 仪表板数据传输对象
 * 用于 API 响应，将 Dashboard 实体转换为 DTO 返回给客户端
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表板响应")
public class DashboardDTO {

    @Schema(description = "仪表板ID")
    private Long id;

    @Schema(description = "仪表板名称")
    private String name;

    @Schema(description = "仪表板描述")
    private String description;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "是否公开")
    private Boolean isPublic;

    @Schema(description = "仪表板配置(JSON格式)")
    private String config;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
