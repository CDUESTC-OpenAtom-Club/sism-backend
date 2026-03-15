package com.sism.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 组织类型枚举
 * 定义了系统中的核心组织分类
 *
 * Values must match PostgreSQL enum: org_type
 */
public enum OrgType {
    /**
     * 系统管理层 - 代表最高级别的管理控制权，通常对应校级或根级组织。
     */
    ADMIN("admin"),

    /**
     * 职能部门 - 代表行政和运营单位，例如：人力资源部、财务部。
     */
    FUNCTIONAL("functional"),

    /**
     * 二级学院/学部 - 代表教学和研究单位，例如：工程学院、文学院。
     */
    ACADEMIC("academic");

    private final String value;

    OrgType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
