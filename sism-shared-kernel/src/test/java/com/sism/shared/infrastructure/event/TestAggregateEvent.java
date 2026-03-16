package com.sism.shared.infrastructure.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TestAggregateEvent - 带聚合根ID的测试事件
 * 用于测试按聚合根ID查询和删除事件的功能
 */
public class TestAggregateEvent extends EventStoreInMemory.AggregateEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateId;
    private final String actionType;
    private final Object payload;

    public TestAggregateEvent(String aggregateId, String actionType, Object payload) {
        super(aggregateId);
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.aggregateId = aggregateId;
        this.actionType = actionType;
        this.payload = payload;
    }

    @JsonCreator
    public TestAggregateEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("actionType") String actionType,
            @JsonProperty("payload") Object payload) {
        super(aggregateId);
        this.eventId = eventId;
        this.occurredOn = occurredOn;
        this.aggregateId = aggregateId;
        this.actionType = actionType;
        this.payload = payload;
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
        return "TestAggregateEvent";
    }

    @Override
    public String getAggregateId() {
        return aggregateId;
    }

    public String getActionType() {
        return actionType;
    }

    public Object getPayload() {
        return payload;
    }
}
