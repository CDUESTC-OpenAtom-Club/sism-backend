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
     * Submit operation (e.g., submit for review)
     */
    SUBMIT,

    /**
     * Approve operation
     */
    APPROVE,

    /**
     * Reject operation
     */
    REJECT,

    /**
     * Archive operation
     */
    ARCHIVE,

    /**
     * Restore operation
     */
    RESTORE
}
