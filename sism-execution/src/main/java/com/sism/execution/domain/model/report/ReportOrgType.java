package com.sism.execution.domain.model.report;

/**
 * ReportOrgType - 报告组织类型枚举
 * 定义报告所属组织的类型
 */
public enum ReportOrgType {
    /**
     * 系统管理员
     */
    SYSTEM_ADMIN,

    /**
     * 职能部门
     */
    FUNCTIONAL,

    /**
     * 学院
     */
    COLLEGE,

    /**
     * 其他 (已废弃，保留以兼容旧数据)
     */
    @Deprecated
    OTHER
}