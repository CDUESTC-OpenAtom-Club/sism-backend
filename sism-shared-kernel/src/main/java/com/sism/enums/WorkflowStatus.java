package com.sism.enums;

/**
 * Workflow status enumeration for the filling distribution workflow system
 * Defines the current workflow lifecycle for indicators
 *
 * Workflow state transitions:
 * DRAFT -> DISTRIBUTED -> PENDING_APPROVAL -> DISTRIBUTED (cycle) -> COMPLETED
 *                      \-> REJECTED -> DRAFT (for modifications)
 *
 * State descriptions:
 * - DRAFT: Initial creation state, indicator being created/edited
 * - DISTRIBUTED: Indicator confirmed and active for progress reporting
 * - PENDING_APPROVAL: Progress report submitted and awaiting approval
 * - REJECTED: Confirmation or approval rejected, requires modification
 * - COMPLETED: Final completion state for end-of-lifecycle indicators
 */
public enum WorkflowStatus {
    /**
     * Draft state (草稿状态)
     * Initial creation state when indicator is being created or edited
     */
    DRAFT,

    /**
     * Distributed and active (已下发)
     * Indicator confirmed by target department and active for progress reporting
     */
    DISTRIBUTED,

    /**
     * Pending approval (待审批)
     * Progress report submitted and awaiting approval from supervising department
     */
    PENDING_APPROVAL,

    /**
     * Rejected (已驳回)
     * Confirmation or approval rejected, requires modification and resubmission
     */
    REJECTED,

    /**
     * Completed (已完成)
     * Final completion state for indicators that have reached end-of-lifecycle
     */
    COMPLETED
}
