package com.sism.shared.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.shared.domain.model.base.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EventStoreDatabase - 数据库持久化的事件存储
 * 将领域事件持久化到数据库，支持事件溯源
 *
 * 激活条件: spring.event-store.persistence=database
 */
@Component("databaseEventStore")
@ConditionalOnProperty(
    name = "spring.event-store.persistence",
    havingValue = "database",
    matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class EventStoreDatabase implements EventStore {

    private final EventStoreRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(DomainEvent event) {
        if (event == null) {
            log.warn("Attempted to save null event");
            throw new IllegalArgumentException("Event cannot be null");
        }

        try {
            // 检查事件是否已存在（幂等性）
            if (repository.existsByEventId(event.getEventId())) {
                log.debug("Event already exists, skipping: {}", event.getEventId());
                return;
            }

            // 序列化事件为 JSON
            String eventData = objectMapper.writeValueAsString(event);

            // 获取聚合根信息
            String aggregateId = extractAggregateId(event);
            String aggregateType = event.getClass().getSimpleName();

            // 创建存储事件记录
            StoredEvent storedEvent = new StoredEvent();
            storedEvent.setEventId(event.getEventId());
            storedEvent.setEventType(event.getEventType());
            storedEvent.setAggregateId(aggregateId);
            storedEvent.setAggregateType(aggregateType);
            storedEvent.setEventData(eventData);
            storedEvent.setOccurredOn(event.getOccurredOn());
            storedEvent.setCreatedAt(LocalDateTime.now());
            storedEvent.setIsProcessed(false);

            // 保存到数据库
            repository.save(storedEvent);

            log.debug("Event saved successfully: {} (type: {})",
                    event.getEventId(), event.getEventType());

        } catch (Exception e) {
            log.error("Failed to save event: {} - {}", event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save event to database: " + event.getEventType(), e);
        }
    }

    @Override
    public Optional<DomainEvent> findById(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return Optional.empty();
        }

        try {
            return repository.findByEventId(eventId)
                    .map(this::deserializeEvent);
        } catch (Exception e) {
            log.error("Failed to find event by ID: {}", eventId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<DomainEvent> findByAggregateId(String aggregateId) {
        if (aggregateId == null || aggregateId.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return repository.findByAggregateIdOrderByCreatedAt(aggregateId)
                    .stream()
                    .map(this::deserializeEvent)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find events by aggregate ID: {}", aggregateId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<DomainEvent> findByEventType(String eventType) {
        if (eventType == null || eventType.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return repository.findByEventType(eventType)
                    .stream()
                    .map(this::deserializeEvent)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find events by type: {}", eventType, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<DomainEvent> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || start.isAfter(end)) {
            return Collections.emptyList();
        }

        try {
            return repository.findByOccurredOnBetween(start, end)
                    .stream()
                    .map(this::deserializeEvent)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find events in time range: {} to {}", start, end, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void delete(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return;
        }

        try {
            repository.findByEventId(eventId).ifPresent(repository::delete);
            log.debug("Event deleted: {}", eventId);
        } catch (Exception e) {
            log.error("Failed to delete event: {}", eventId, e);
            throw new RuntimeException("Failed to delete event: " + eventId, e);
        }
    }

    @Override
    public void deleteByAggregateId(String aggregateId) {
        if (aggregateId == null || aggregateId.isEmpty()) {
            return;
        }

        try {
            List<StoredEvent> events = repository.findByAggregateId(aggregateId);
            if (!events.isEmpty()) {
                repository.deleteAll(events);
                log.debug("Deleted {} events for aggregate: {}", events.size(), aggregateId);
            }
        } catch (Exception e) {
            log.error("Failed to delete events for aggregate: {}", aggregateId, e);
            throw new RuntimeException("Failed to delete events: " + aggregateId, e);
        }
    }

    /**
     * 获取未处理的事件
     */
    public List<DomainEvent> findUnprocessedEvents() {
        try {
            return repository.findByIsProcessedFalse()
                    .stream()
                    .map(this::deserializeEvent)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find unprocessed events", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取未处理的指定类型事件
     */
    public List<DomainEvent> findUnprocessedEventsByType(String eventType) {
        if (eventType == null || eventType.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return repository.findByEventTypeAndIsProcessedFalse(eventType)
                    .stream()
                    .map(this::deserializeEvent)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find unprocessed events of type: {}", eventType, e);
            return Collections.emptyList();
        }
    }

    /**
     * 标记事件已处理
     */
    public void markEventAsProcessed(String eventId) {
        try {
            repository.findByEventId(eventId).ifPresent(event -> {
                event.markAsProcessed();
                repository.save(event);
                log.debug("Event marked as processed: {}", eventId);
            });
        } catch (Exception e) {
            log.error("Failed to mark event as processed: {}", eventId, e);
        }
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 总事件数
            long totalCount = repository.count();
            stats.put("totalEvents", totalCount);

            // 未处理事件数
            long unprocessedCount = repository.countByIsProcessedFalse();
            stats.put("unprocessedEvents", unprocessedCount);

            // 处理成功率
            if (totalCount > 0) {
                double successRate = ((double) (totalCount - unprocessedCount) / totalCount) * 100;
                stats.put("successRate", String.format("%.2f%%", successRate));
            }

        } catch (Exception e) {
            log.error("Failed to get statistics", e);
        }

        return stats;
    }

    /**
     * 反序列化事件（内部方法）
     */
    private DomainEvent deserializeEvent(StoredEvent stored) {
        try {
            String eventType = stored.getEventType();
            Class<?> eventClass = getEventClass(eventType);

            if (eventClass != null) {
                return (DomainEvent) objectMapper.readValue(stored.getEventData(), eventClass);
            } else {
                log.warn("Unknown event type, cannot deserialize: {}", eventType);
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to deserialize event: {}", stored.getEventId(), e);
            return null;
        }
    }

    /**
     * 根据事件类型名称获取事件类
     */
    private Class<?> getEventClass(String eventType) {
        // 尝试在测试包中查找
        String[] searchPackages = {
            "com.sism.shared.infrastructure.event",  // 测试事件
            "com.sism.strategy.domain.model.indicator.event",
            "com.sism.execution.domain.model.report.event",
            "com.sism.shared.domain.model.event"
        };

        for (String pkg : searchPackages) {
            try {
                return Class.forName(pkg + "." + eventType);
            } catch (ClassNotFoundException e) {
                // 继续尝试下一个包
            }
        }

        log.debug("Event class not found for type: {}", eventType);
        return null;
    }

    /**
     * 提取聚合根ID（从事件中提取）
     */
    private String extractAggregateId(DomainEvent event) {
        try {
            // 尝试通过反射获取聚合根 ID
            java.lang.reflect.Field[] fields = event.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (field.getName().endsWith("Id") &&
                    (field.getType().equals(Long.class) || field.getType().equals(String.class))) {
                    field.setAccessible(true);
                    Object value = field.get(event);
                    return value != null ? value.toString() : null;
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract aggregate ID from event: {}", e.getMessage());
        }
        return null;
    }
}
