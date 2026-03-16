package com.sism.organization.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 组织类型枚举 (Organization Bounded Context)
 * 定义了本组织上下文中组织可以拥有的类型
 *
 * 必须与 com.sism.enums.OrgType 保持一致
 * 
 * Values must match PostgreSQL enum: org_type
 */
public enum OrgType {
    /**
     * 系统管理层
     * 代表最高级别的管理控制权，通常对应校级或根级组织
     */
    ADMIN("admin"),

    /**
     * 职能部门
     * 代表行政和运营单位，例如：人力资源部、财务部
     */
    FUNCTIONAL("functional"),

    /**
     * 二级学院/学部
     * 代表教学和研究单位，例如：工程学院、文学院
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

    /**
     * 从字符串值转换为枚举
     */
    public static OrgType fromValue(String value) {
        for (OrgType type : OrgType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid OrgType value: " + value);
    }

    /**
     * 转换为 shared-kernel 中的 OrgType
     */
    public com.sism.enums.OrgType toSharedOrgType() {
        return com.sism.enums.OrgType.valueOf(this.name());
    }

    /**
     * 从 shared-kernel 的 OrgType 转换过来
     */
    public static OrgType fromSharedOrgType(com.sism.enums.OrgType sharedOrgType) {
        if (sharedOrgType == null) {
            return null;
        }
        return OrgType.valueOf(sharedOrgType.name());
    }
}
