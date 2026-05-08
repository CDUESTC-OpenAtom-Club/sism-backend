package com.sism.strategy.domain.indicator;

/**
 * Indicator lifecycle status enumeration
 * Defines the three-state lifecycle of indicators
 *
 * Three-state lifecycle flow:
 * DRAFT -> PENDING -> DISTRIBUTED
 *
 * State descriptions:
 * - DRAFT: Indicator is being created/edited, not yet distributed
 * - PENDING: Indicator distributed, awaiting target organization confirmation
 * - DISTRIBUTED: Indicator confirmed by target organization, active for progress tracking
 *
 * Note: This lifecycle status (status field) is separate from progress approval status
 * (progressApprovalStatus field).
 */
public enum IndicatorStatus {
    /**
     * Draft indicator (草稿)
     * Initial state when indicator is created
     */
    DRAFT,

    /**
     * Pending (待审批 - distributed and awaiting confirmation from target organization)
     * Indicator has been distributed to target organization, awaiting their confirmation
     */
    PENDING,

    /**
     * Distributed (已下发 - confirmed by target organization)
     * Indicator is active and target organization can submit progress data
     */
    DISTRIBUTED
}
