package com.sism.strategy.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sism.shared.domain.model.base.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event emitted when a plan changes status.
 */
@Getter
public class PlanStatusChangedEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final Long planId;
    private final String oldStatus;
    private final String newStatus;

    public PlanStatusChangedEvent(Long planId, String oldStatus, String newStatus) {
        this(UUID.randomUUID().toString(), LocalDateTime.now(), planId, oldStatus, newStatus);
    }

    @JsonCreator
    public PlanStatusChangedEvent(@JsonProperty("eventId") String eventId,
                                  @JsonProperty("occurredOn") LocalDateTime occurredOn,
                                  @JsonProperty("planId") Long planId,
                                  @JsonProperty("oldStatus") String oldStatus,
                                  @JsonProperty("newStatus") String newStatus) {
        this.eventId = eventId;
        this.occurredOn = occurredOn;
        this.planId = planId;
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
        return "PlanStatusChangedEvent";
    }
}
