package com.sism.strategy.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sism.shared.domain.model.base.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event raised when a new plan is created in the strategy context.
 */
@Getter
public class PlanCreatedEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final Long planId;
    private final String planLevel;
    private final Long targetOrgId;

    public PlanCreatedEvent(Long planId, String planLevel, Long targetOrgId) {
        this(UUID.randomUUID().toString(), LocalDateTime.now(), planId, planLevel, targetOrgId);
    }

    @JsonCreator
    public PlanCreatedEvent(@JsonProperty("eventId") String eventId,
                            @JsonProperty("occurredOn") LocalDateTime occurredOn,
                            @JsonProperty("planId") Long planId,
                            @JsonProperty("planLevel") String planLevel,
                            @JsonProperty("targetOrgId") Long targetOrgId) {
        this.eventId = eventId;
        this.occurredOn = occurredOn;
        this.planId = planId;
        this.planLevel = planLevel;
        this.targetOrgId = targetOrgId;
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
        return "PlanCreatedEvent";
    }
}
