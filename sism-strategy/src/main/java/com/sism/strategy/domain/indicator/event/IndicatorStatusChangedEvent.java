package com.sism.strategy.domain.indicator.event;

import com.sism.shared.domain.model.base.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class IndicatorStatusChangedEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final Long indicatorId;
    private final String oldStatus;
    private final String newStatus;

    public IndicatorStatusChangedEvent(Long indicatorId, String oldStatus, String newStatus) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.indicatorId = indicatorId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
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
        return "INDICATOR_STATUS_CHANGED";
    }
}
