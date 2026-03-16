package com.sism.shared.infrastructure.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sism.shared.domain.model.base.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TestDomainEvent - 纯测试用的领域事件
 * 仅用于事件基础设施的单元测试，不依赖任何业务模块
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestDomainEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String testData;
    private final Long entityId;

    public TestDomainEvent(String testData, Long entityId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.testData = testData;
        this.entityId = entityId;
    }

    @JsonCreator
    public TestDomainEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("testData") String testData,
            @JsonProperty("entityId") Long entityId) {
        this.eventId = eventId;
        this.occurredOn = occurredOn;
        this.testData = testData;
        this.entityId = entityId;
    }

    /**
     * Constructor with custom eventId for testing edge cases
     */
    public TestDomainEvent(String customEventId, String eventId, LocalDateTime occurredOn, String testData, Long entityId) {
        this.eventId = customEventId;
        this.occurredOn = occurredOn;
        this.testData = testData;
        this.entityId = entityId;
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
        return "TestDomainEvent";
    }

    public String getTestData() {
        return testData;
    }

    public Long getEntityId() {
        return entityId;
    }
}
