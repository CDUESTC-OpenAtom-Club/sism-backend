package com.sism.shared.infrastructure.event;

import com.sism.shared.domain.model.base.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 领域事件发布器
 * 负责收集并发布领域事件
 */
@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final EventStore eventStore;

    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                                ObjectProvider<EventStore> eventStoreProvider) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.eventStore = eventStoreProvider.getIfAvailable();
    }

    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher, EventStore eventStore) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.eventStore = eventStore;
    }

    /**
     * 发布单个领域事件
     */
    public void publish(DomainEvent event) {
        if (event == null) {
            log.warn("Attempted to publish null event");
            return;
        }

        log.debug("Publishing domain event: {}", event.getEventType());

        if (eventStore != null) {
            try {
                eventStore.save(event);
            } catch (Exception e) {
                log.warn("Failed to persist event {}, continuing with in-process publish: {}",
                        event.getEventId(), e.getMessage());
            }
        }

        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布多个领域事件
     */
    public void publishAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        List<DomainEvent> publishedEvents = new ArrayList<>();
        for (DomainEvent event : events) {
            try {
                publish(event);
                publishedEvents.add(event);
            } catch (Exception e) {
                log.error("Failed to publish event in batch: {}", event.getEventId(), e);
                // 继续发布其他事件
            }
        }

        log.info("Published {} events out of {}", publishedEvents.size(), events.size());
    }
}
