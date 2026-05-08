package com.sism.shared.domain.model.base;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 领域事件接口
 * DDD中领域事件表示领域中发生的事情
 */
public interface DomainEvent {

    /**
     * 获取事件ID
     */
    default String getEventId() {
        return DomainEventState.eventId(this);
    }

    /**
     * 获取事件发生时间
     */
    default LocalDateTime getOccurredOn() {
        return DomainEventState.occurredOn(this);
    }

    /**
     * 获取事件类型
     */
    String getEventType();

    /**
     * Memoizes default event metadata for event implementations that do not store it explicitly.
     * Weak keys avoid retaining event instances after they are no longer referenced elsewhere.
     */
    final class DomainEventState {
        private static final Map<DomainEvent, String> EVENT_IDS =
                Collections.synchronizedMap(new WeakHashMap<>());
        private static final Map<DomainEvent, LocalDateTime> OCCURRED_ON =
                Collections.synchronizedMap(new WeakHashMap<>());

        private DomainEventState() {
        }

        static String eventId(DomainEvent event) {
            synchronized (EVENT_IDS) {
                return EVENT_IDS.computeIfAbsent(event, ignored -> UUID.randomUUID().toString());
            }
        }

        static LocalDateTime occurredOn(DomainEvent event) {
            synchronized (OCCURRED_ON) {
                return OCCURRED_ON.computeIfAbsent(event, ignored -> LocalDateTime.now());
            }
        }
    }
}
