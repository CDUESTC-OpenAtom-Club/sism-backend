package com.sism.organization.domain.event;

import com.sism.shared.domain.model.base.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class OrgCreatedEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final Long orgId;
    private final String orgName;
    private final String orgType;

    public OrgCreatedEvent(Long orgId, String orgName, String orgType) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.orgId = orgId;
        this.orgName = orgName;
        this.orgType = orgType;
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
        return "ORG_CREATED";
    }
}
