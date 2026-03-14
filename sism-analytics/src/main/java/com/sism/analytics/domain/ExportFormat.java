package com.sism.analytics.domain;

import java.util.Arrays;
import java.util.List;

/**
 * ExportFormat - 导出格式枚举
 * 定义系统支持的数据导出格式
 */
public enum ExportFormat {

    PDF("PDF", "PDF文档", "application/pdf", ".pdf"),
    EXCEL("EXCEL", "Excel表格", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
    CSV("CSV", "CSV文件", "text/csv", ".csv"),
    HTML("HTML", "HTML页面", "text/html", ".html");

    private final String code;
    private final String displayName;
    private final String mimeType;
    private final String fileExtension;

    ExportFormat(String code, String displayName, String mimeType, String fileExtension) {
        this.code = code;
        this.displayName = displayName;
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * 根据代码获取导出格式
     */
    public static ExportFormat fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Code cannot be null");
        }

        for (ExportFormat format : values()) {
            if (format.code.equals(code)) {
                return format;
            }
        }

        throw new IllegalArgumentException("Unknown export format code: " + code);
    }

    /**
     * 检查是否是有效的导出格式代码
     */
    public static boolean isValidCode(String code) {
        if (code == null) {
            return false;
        }

        for (ExportFormat format : values()) {
            if (format.code.equals(code)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取所有支持的导出格式
     */
    public static List<ExportFormat> getAllSupportedFormats() {
        return Arrays.asList(values());
    }

    /**
     * 获取表格数据支持的导出格式
     */
    public static List<ExportFormat> getTableSupportedFormats() {
        return Arrays.asList(EXCEL, CSV, PDF);
    }

    /**
     * 获取图表数据支持的导出格式
     */
    public static List<ExportFormat> getChartSupportedFormats() {
        return Arrays.asList(PDF, EXCEL, HTML);
    }
}
