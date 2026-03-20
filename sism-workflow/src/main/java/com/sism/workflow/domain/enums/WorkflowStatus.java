package com.sism.workflow.domain.enums;

/**
 * Historical compatibility enum retained for legacy workflow lifecycle mappings.
 *
 * The current approval instance runtime state is defined by {@code AuditInstance}:
 * IN_REVIEW / APPROVED / REJECTED, while requester cancellation is handled as an
 * internal terminal state and should not be exposed as a public approval status.
 *
 * New approval-flow code must not use this enum as the source of truth for
 * approval instance status transitions.
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
