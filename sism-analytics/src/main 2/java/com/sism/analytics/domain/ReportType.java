package com.sism.analytics.domain;

/**
 * ReportType - 报告类型枚举
 * 定义系统支持的分析报告类型
 */
public enum ReportType {

    STRATEGIC("STRATEGIC", "战略分析报告", "analysis.strategic"),
    EXECUTION("EXECUTION", "执行分析报告", "analysis.execution"),
    FINANCIAL("FINANCIAL", "财务分析报告", "analysis.financial"),
    OPERATIONAL("OPERATIONAL", "运营分析报告", "analysis.operational"),
    COMPREHENSIVE("COMPREHENSIVE", "综合分析报告", "analysis.comprehensive");

    private final String code;
    private final String displayName;
    private final String i18nKey;

    ReportType(String code, String displayName, String i18nKey) {
        this.code = code;
        this.displayName = displayName;
        this.i18nKey = i18nKey;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    /**
     * 根据代码获取报告类型
     */
    public static ReportType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Code cannot be null");
        }

        for (ReportType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown report type code: " + code);
    }

    /**
     * 检查是否是有效的报告类型代码
     */
    public static boolean isValidCode(String code) {
        if (code == null) {
            return false;
        }

        for (ReportType type : values()) {
            if (type.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}
