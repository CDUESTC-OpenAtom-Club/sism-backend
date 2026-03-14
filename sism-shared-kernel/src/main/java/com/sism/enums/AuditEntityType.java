package com.sism.enums;

/**
 * Audit entity type enumeration
 * Defines the types of entities that can be audited
 * 
 * Values must match PostgreSQL enum: audit_entity_type
 */
public enum AuditEntityType {
    /**
     * Organization entity (org table)
     */
    ORG,
    
    /**
     * User entity (app_user table)
     */
    USER,
    
    /**
     * Assessment cycle entity (assessment_cycle table)
     */
    CYCLE,
    
    /**
     * Strategic task entity (strategic_task table)
     */
    TASK,
    
    /**
     * Indicator entity (indicator table)
     */
    INDICATOR,
    
    /**
     * Milestone entity (milestone table)
     */
    MILESTONE,
    
    /**
     * Progress report entity (progress_report table)
     */
    REPORT,
    
    /**
     * Adhoc task entity (adhoc_task table)
     */
    ADHOC_TASK,
    
    /**
     * Alert entity (alert_event table)
     */
    ALERT,
    
    /**
     * Plan entity (plan table)
     */
    PLAN
}
