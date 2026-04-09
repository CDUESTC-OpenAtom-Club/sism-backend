package com.sism.alert.domain.event;

import com.sism.shared.domain.model.base.DomainEvent;

public record AlertResolvedEvent(
        Long alertId,
        Long indicatorId,
        Long handledBy,
        String status
) implements DomainEvent {

    @Override
    public String getEventType() {
        return "AlertResolvedEvent";
    }
}
