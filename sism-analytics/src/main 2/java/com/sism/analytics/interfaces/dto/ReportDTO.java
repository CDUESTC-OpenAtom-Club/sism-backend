package com.sism.analytics.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ReportDTO - 分析报告数据传输对象
 * 用于 API 响应，将 Report 实体转换为 DTO 返回给客户端
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

    /**
     * 报告ID
     */
    private Long id;

    /**
     * 报告名称
     */
    private String name;

    /**
     * 报告类型 (STRATEGIC, EXECUTION, FINANCIAL, OPERATIONAL, COMPREHENSIVE)
     */
    private String type;

    /**
     * 报告格式 (PDF, EXCEL, CSV, HTML)
     */
    private String format;

    /**
     * 报告状态 (DRAFT, GENERATED, FAILED)
     */
    private String status;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 生成者ID
     */
    private Long generatedBy;

    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 报告参数（JSON格式）
     */
    private String parameters;

    /**
     * 报告描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
