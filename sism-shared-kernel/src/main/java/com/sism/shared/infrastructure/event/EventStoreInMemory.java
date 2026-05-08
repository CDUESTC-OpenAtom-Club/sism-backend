package com.sism.shared.infrastructure.event;

import com.sism.shared.domain.model.base.DomainEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
@ConditionalOnMissingBean(EventStore.class)
public class EventStoreInMemory implements EventStore {

    private final Map<String, DomainEvent> events = new ConcurrentHashMap<>();
    private final Map<String, List<DomainEvent>> aggregateEvents = new ConcurrentHashMap<>();

    @Override
    public void save(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        String eventId = event.getEventId();
        events.put(eventId, event);

        if (event instanceof AggregateEvent) {
            String aggregateId = ((AggregateEvent) event).getAggregateId();
            aggregateEvents.computeIfAbsent(aggregateId, k -> new CopyOnWriteArrayList<>()).add(event);
        }
    }

    @Override
    public Optional<DomainEvent> findById(String eventId) {
        return Optional.ofNullable(events.get(eventId));
    }

    @Override
    public List<DomainEvent> findByAggregateId(String aggregateId) {
        return aggregateEvents.getOrDefault(aggregateId, Collections.emptyList());
    }

    @Override
    public List<DomainEvent> findByEventType(String eventType) {
        return events.values().stream()
                .filter(event -> eventType.equals(event.getEventType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainEvent> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        return events.values().stream()
                .filter(event -> {
                    LocalDateTime occurredOn = event.getOccurredOn();
                    return !occurredOn.isBefore(start) && !occurredOn.isAfter(end);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String eventId) {
        DomainEvent event = events.remove(eventId);
        if (event instanceof AggregateEvent) {
            String aggregateId = ((AggregateEvent) event).getAggregateId();
            List<DomainEvent> eventsForAggregate = aggregateEvents.get(aggregateId);
            if (eventsForAggregate != null) {
                eventsForAggregate.remove(event);
            }
        }
    }

    @Override
    public void deleteByAggregateId(String aggregateId) {
        List<DomainEvent> eventsForAggregate = aggregateEvents.remove(aggregateId);
        if (eventsForAggregate != null) {
            for (DomainEvent event : eventsForAggregate) {
                events.remove(event.getEventId());
            }
        }
    }

    public void clear() {
        events.clear();
        aggregateEvents.clear();
    }

    public int count() {
        return events.size();
    }

    public Map<String, Long> getStatistics() {
        return events.values().stream()
                .collect(Collectors.groupingBy(DomainEvent::getEventType, Collectors.counting()));
    }

    public abstract static class AggregateEvent implements DomainEvent {
        private final String aggregateId;

        public AggregateEvent(String aggregateId) {
            this.aggregateId = aggregateId;
        }

        public String getAggregateId() {
            return aggregateId;
        }
    }
}
