package com.sism.strategy.domain.indicator.event;

import com.sism.shared.domain.model.base.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class IndicatorCreatedEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final Long indicatorId;
    private final String indicatorDesc;
    private final Long ownerOrgId;

    public IndicatorCreatedEvent(Long indicatorId, String indicatorDesc, Long ownerOrgId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.indicatorId = indicatorId;
        this.indicatorDesc = indicatorDesc;
        this.ownerOrgId = ownerOrgId;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return "INDICATOR_CREATED";
    }
}
