package com.sism.strategy.domain.event;

import com.sism.shared.domain.model.base.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event raised when a plan is submitted and should start a workflow instance.
 */
@Getter
@AllArgsConstructor
public class PlanSubmittedForApprovalEvent implements DomainEvent {

    private final Long planId;
    private final String workflowCode;
    private final Long submitterId;
    private final Long submitterOrgId;

    @Override
    public String getEventType() {
        return "PlanSubmittedForApprovalEvent";
    }
}
