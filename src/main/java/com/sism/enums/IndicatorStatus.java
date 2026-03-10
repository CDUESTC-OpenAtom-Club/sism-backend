package com.sism.enums;

/**
 * Indicator lifecycle status enumeration
 * Defines the four-state lifecycle of indicators
 * 
 * Four-state lifecycle flow:
 * DRAFT -> PENDING_REVIEW -> DISTRIBUTED -> ARCHIVED
 * 
 * State descriptions:
 * - DRAFT: Indicator is being created/edited, not yet submitted for review
 * - PENDING_REVIEW: Indicator submitted and awaiting strategic dept approval of definition
 * - DISTRIBUTED: Indicator approved and distributed to departments for progress tracking
 * - ARCHIVED: Indicator soft-deleted or end-of-lifecycle
 * 
 * Note: This lifecycle status (status field) is separate from progress approval status
 * (progressApprovalStatus field). PENDING_REVIEW represents indicator definition review,
 * while progressApprovalStatus.PENDING represents progress submission approval.
 */
public enum IndicatorStatus {
    /**
     * Draft indicator (草稿 - not yet submitted for review)
     * Initial state when indicator is created
     */
    DRAFT,
    
    /**
     * Pending review (待审核 - submitted and awaiting strategic dept approval)
     * Indicator definition is under review by strategic department
     * This is distinct from progress approval (progressApprovalStatus.PENDING)
     */
    PENDING_REVIEW,
    
    /**
     * Pending review (待审核 - submitted and awaiting strategic dept approval)
     * @deprecated Use PENDING_REVIEW instead to clearly distinguish indicator definition review
     * from progress approval workflow. This enum value will be removed in a future version.
     * Migration: All PENDING values should be converted to PENDING_REVIEW.
     */
    @Deprecated
    PENDING,
    
    /**
     * Distributed (已下发 - approved and distributed to departments)
     * Indicator is active and departments can submit progress data
     */
    DISTRIBUTED,
    
    /**
     * Active indicator (运行中 - legacy status, equivalent to DISTRIBUTED)
     * @deprecated Use DISTRIBUTED for new indicators
     */
    @Deprecated
    ACTIVE,
    
    /**
     * Archived/soft-deleted indicator (已归档)
     * End-of-lifecycle state for indicators no longer in use
     */
    ARCHIVED
}
