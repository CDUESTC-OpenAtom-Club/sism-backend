package com.sism.execution.domain.event;

import com.sism.shared.domain.model.base.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event raised when a new plan is created
 */
@Getter
@AllArgsConstructor
public class PlanCreatedEvent implements DomainEvent {
    private Long planId;
    private String planLevel;
    private Long targetOrgId;

    @Override
    public String getEventType() {
        return "PlanCreatedEvent";
    }
}
