package com.sism.analytics.application;

import com.sism.shared.domain.model.base.AggregateRoot;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

abstract class BaseApplicationService {

    protected void requirePositiveUserId(Long userId, String fieldName) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive number");
        }
    }

    protected void requireUserOwnership(Long requestedUserId, Long currentUserId, String errorMessage) {
        requirePositiveUserId(requestedUserId, "Requested user ID");
        requirePositiveUserId(currentUserId, "Current user ID");
        if (!Objects.equals(requestedUserId, currentUserId)) {
            throw new AccessDeniedException(errorMessage != null ? errorMessage : "No permission to access this resource");
        }
    }

    protected void requireUserOwnership(Long requestedUserId, Long currentUserId) {
        requireUserOwnership(requestedUserId, currentUserId, null);
    }

    protected void requireValidDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }

    protected String escapeLikePattern(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Search value cannot be null");
        }
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    protected void publishAndSaveEvents(
            AggregateRoot<?> aggregateRoot,
            EventStore eventStore,
            DomainEventPublisher eventPublisher
    ) {
        List<DomainEvent> events = aggregateRoot.getDomainEvents();
        if (events == null || events.isEmpty()) {
            return;
        }
        List<String> savedEventIds = new ArrayList<>();
        try {
            for (DomainEvent event : events) {
                eventStore.save(event);
                savedEventIds.add(event.getEventId());
            }
            eventPublisher.publishAll(events);
            aggregateRoot.clearEvents();
        } catch (RuntimeException ex) {
            rollbackSavedEvents(eventStore, savedEventIds);
            throw ex;
        }
    }

    private void rollbackSavedEvents(EventStore eventStore, List<String> savedEventIds) {
        for (int i = savedEventIds.size() - 1; i >= 0; i--) {
            try {
                eventStore.delete(savedEventIds.get(i));
            } catch (RuntimeException ignored) {
                // Best-effort compensation for in-memory or non-transactional event stores.
            }
        }
    }
}
