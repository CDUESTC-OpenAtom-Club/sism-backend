package com.sism.execution.domain.model.report;

/**
 * ReportOrgType - 报告组织类型枚举
 * 定义报告所属组织的类型
 *
 * 必须与 com.sism.enums.OrgType 保持一致
 */
public enum ReportOrgType {
    /**
     * 系统管理员
     */
    ADMIN,

    /**
     * 职能部门
     */
    FUNCTIONAL,

    /**
     * 二级学院
     */
    ACADEMIC
}
