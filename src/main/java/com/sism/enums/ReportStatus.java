package com.sism.enums;

/**
 * Progress report status enumeration
 * Defines the workflow status of progress reports
 */
public enum ReportStatus {
    /**
     * Draft status - initial state
     */
    DRAFT,
    
    /**
     * Submitted status - awaiting approval
     */
    SUBMITTED,
    
    /**
     * Returned status - returned for modification
     */
    RETURNED,
    
    /**
     * Approved status - approved by reviewer
     */
    APPROVED,
    
    /**
     * Rejected status - rejected by reviewer
     */
    REJECTED
}
