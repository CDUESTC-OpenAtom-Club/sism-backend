package com.sism.analytics.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DataExportDTO - 数据导出数据传输对象
 * 用于 API 响应，将 DataExport 实体转换为 DTO 返回给客户端
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据导出响应")
public class DataExportDTO {

    @Schema(description = "导出任务ID")
    private Long id;

    @Schema(description = "导出任务名称")
    private String name;

    @Schema(description = "导出类型")
    private String type;

    @Schema(description = "导出格式")
    private String format;

    @Schema(description = "导出状态")
    private String status;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "请求者ID")
    private Long requestedBy;

    @Schema(description = "请求时间")
    private LocalDateTime requestedAt;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "导出参数(JSON格式)")
    private String parameters;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
