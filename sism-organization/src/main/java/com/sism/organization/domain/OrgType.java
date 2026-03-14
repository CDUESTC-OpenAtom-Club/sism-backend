package com.sism.organization.domain;

/**
 * Organization type enumeration
 * Defines the different types of organizations in the system
 *
 * Values must match PostgreSQL enum: org_type
 */
public enum OrgType {
    /**
     * School-level organization
     */
    SCHOOL,

    /**
     * Functional department
     */
    FUNCTIONAL_DEPT,

    /**
     * Function department (alternative naming)
     */
    FUNCTION_DEPT,

    /**
     * College/Faculty
     */
    COLLEGE,

    /**
     * Strategy department
     */
    STRATEGY_DEPT,

    /**
     * Division (sub-unit of college)
     */
    DIVISION,

    /**
     * Other organization types
     */
    OTHER
}
