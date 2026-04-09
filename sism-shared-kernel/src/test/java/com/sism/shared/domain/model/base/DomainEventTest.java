package com.sism.shared.domain.model.base;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DomainEventTest {

    @Test
    void defaultMetadataShouldRemainStablePerInstance() {
        DummyEvent event = new DummyEvent();

        String firstEventId = event.getEventId();
        String secondEventId = event.getEventId();
        LocalDateTime firstOccurredOn = event.getOccurredOn();
        LocalDateTime secondOccurredOn = event.getOccurredOn();

        assertEquals(firstEventId, secondEventId);
        assertSame(firstOccurredOn, secondOccurredOn);
    }

    private static final class DummyEvent implements DomainEvent {
        @Override
        public String getEventType() {
            return "DUMMY_EVENT";
        }
    }
}
