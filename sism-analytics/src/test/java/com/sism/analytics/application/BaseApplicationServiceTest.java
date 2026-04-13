package com.sism.analytics.application;

import com.sism.shared.domain.model.base.AggregateRoot;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BaseApplicationService Tests")
class BaseApplicationServiceTest {

    @Test
    @DisplayName("publishAndSaveEvents should compensate saved events when later save fails")
    void publishAndSaveEventsShouldCompensateSavedEventsWhenLaterSaveFails() {
        EventStore eventStore = mock(EventStore.class);
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        TestAggregate aggregate = new TestAggregate();

        RuntimeException failure = new RuntimeException("event store failed");
        org.mockito.Mockito.doThrow(failure)
                .when(eventStore)
                .save(org.mockito.ArgumentMatchers.argThat(event -> "event-2".equals(event.getEventId())));

        assertThrows(RuntimeException.class, () ->
                new TestApplicationService().publish(aggregate, eventStore, publisher)
        );

        verify(eventStore).save(org.mockito.ArgumentMatchers.argThat(event -> "event-1".equals(event.getEventId())));
        verify(eventStore).delete("event-1");
        verify(eventStore, times(0)).delete("event-2");
    }

    private static final class TestApplicationService extends BaseApplicationService {
        void publish(TestAggregate aggregate, EventStore eventStore, DomainEventPublisher publisher) {
            publishAndSaveEvents(aggregate, eventStore, publisher);
        }
    }

    private static final class TestAggregate extends AggregateRoot<Long> {
        TestAggregate() {
            addEvent(new TestEvent("event-1"));
            addEvent(new TestEvent("event-2"));
        }

        @Override
        public void validate() {
        }
    }

    private static final class TestEvent implements DomainEvent {
        private final String eventId;

        private TestEvent(String eventId) {
            this.eventId = eventId;
        }

        @Override
        public String getEventId() {
            return eventId;
        }

        @Override
        public String getEventType() {
            return "TestEvent";
        }
    }
}
