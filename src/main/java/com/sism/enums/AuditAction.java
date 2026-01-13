package com.sism.enums;

/**
 * Audit action enumeration
 * Defines the types of auditable actions in the system
 */
public enum AuditAction {
    /**
     * Create operation
     */
    CREATE,
    
    /**
     * Update operation
     */
    UPDATE,
    
    /**
     * Delete operation
     */
    DELETE,
    
    /**
     * Approve operation
     */
    APPROVE,
    
    /**
     * Archive operation
     */
    ARCHIVE,
    
    /**
     * Restore operation
     */
    RESTORE
}
