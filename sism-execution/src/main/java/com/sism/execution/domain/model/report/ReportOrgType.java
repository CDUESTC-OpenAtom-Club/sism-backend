package com.sism.execution.domain.model.report;

/**
 * ReportOrgType - 报告组织类型枚举
 * 定义报告所属组织的类型
 *
 * IMPORTANT: Enum constant names must match database constraint values
 * Database check constraint: (report_org_type::text = ANY (ARRAY['FUNC_DEPT'::character varying, 'COLLEGE'::character varying]::text[]))
 */
public enum ReportOrgType {
    /**
     * 职能部门 - FUNC_DEPT
     */
    FUNC_DEPT,

    /**
     * 二级学院 - COLLEGE
     */
    COLLEGE
}
