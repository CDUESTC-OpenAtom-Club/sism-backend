package com.sism.enums;

/**
 * Adhoc task scope type enumeration
 * Defines how adhoc tasks are scoped to organizations/indicators
 */
public enum AdhocScopeType {
    /**
     * All designated organizations
     */
    ALL_ORGS,
    
    /**
     * Indicators issued by the department
     */
    BY_DEPT_ISSUED_INDICATORS,
    
    /**
     * Custom manual selection
     */
    CUSTOM
}
