package com.sism.alert.domain.event;

import com.sism.shared.domain.model.base.DomainEvent;

public record AlertCreatedEvent(
        Long alertId,
        Long indicatorId,
        String severity,
        String status
) implements DomainEvent {

    @Override
    public String getEventType() {
        return "AlertCreatedEvent";
    }
}
