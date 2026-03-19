package com.sism.workflow.application.support;

import com.sism.shared.domain.model.base.AggregateRoot;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkflowEventDispatcher {

    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;

    public void publish(AggregateRoot<?> aggregate) {
        List<DomainEvent> events = aggregate.getDomainEvents();
        for (DomainEvent event : events) {
            eventStore.save(event);
        }
        eventPublisher.publishAll(events);
        aggregate.clearEvents();
    }
}
