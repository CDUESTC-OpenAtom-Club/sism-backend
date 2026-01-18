package com.sism.enums;

/**
 * Adhoc task status enumeration
 * Defines the lifecycle status of adhoc tasks
 * 
 * Values must match PostgreSQL enum: adhoc_task_status
 */
public enum AdhocTaskStatus {
    /**
     * Task is in draft state - not yet published
     */
    DRAFT,
    
    /**
     * Task is open and active for reporting
     */
    OPEN,
    
    /**
     * Task is closed - no more reports accepted
     */
    CLOSED,
    
    /**
     * Task is archived - historical record
     */
    ARCHIVED
}
