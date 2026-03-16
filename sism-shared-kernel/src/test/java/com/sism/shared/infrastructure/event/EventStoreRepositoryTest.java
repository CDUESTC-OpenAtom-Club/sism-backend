package com.sism.shared.infrastructure.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * EventStoreRepositoryTest - 事件存储Repository的集成测试
 *
 * 使用H2内存数据库进行测试，不影响任何业务表
 *
 * 测试覆盖:
 * - 基本CRUD操作
 * - 自定义查询方法
 * - 分页查询
 * - 统计方法
 */
@DataJpaTest
@ActiveProfiles("test")
@EnableAutoConfiguration
@DisplayName("EventStoreRepository 集成测试")
class EventStoreRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventStoreRepository repository;

    private StoredEvent createTestEvent(String eventId, String eventType, String aggregateId) {
        StoredEvent event = new StoredEvent();
        event.setEventId(eventId != null ? eventId : UUID.randomUUID().toString());
        event.setEventType(eventType != null ? eventType : "TestEvent");
        event.setAggregateId(aggregateId);
        event.setAggregateType("TestAggregate");
        event.setEventData("{\"test\": \"data\"}");
        event.setOccurredOn(LocalDateTime.now());
        event.setCreatedAt(LocalDateTime.now());
        event.setIsProcessed(false);
        return event;
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("基本CRUD操作测试")
    class BasicCrudTests {

        @Test
        @DisplayName("应该成功保存事件")
        void shouldSaveEvent() {
            // Given
            StoredEvent event = createTestEvent(null, "UserCreatedEvent", "user-123");

            // When
            StoredEvent saved = repository.save(event);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getEventId()).isNotNull();
        }

        @Test
        @DisplayName("应该成功读取保存的事件")
        void shouldReadSavedEvent() {
            // Given
            StoredEvent event = createTestEvent(null, "OrderCreatedEvent", "order-456");
            StoredEvent saved = repository.save(event);
            entityManager.flush();
            entityManager.clear();

            // When
            Optional<StoredEvent> found = repository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getEventType()).isEqualTo("OrderCreatedEvent");
            assertThat(found.get().getAggregateId()).isEqualTo("order-456");
        }

        @Test
        @DisplayName("应该成功更新事件")
        void shouldUpdateEvent() {
            // Given
            StoredEvent event = createTestEvent(null, "TestEvent", "agg-1");
            StoredEvent saved = repository.save(event);
            entityManager.flush();

            // When
            saved.markAsProcessed();
            repository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            StoredEvent updated = repository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getIsProcessed()).isTrue();
            assertThat(updated.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("应该成功删除事件")
        void shouldDeleteEvent() {
            // Given
            StoredEvent event = createTestEvent(null, "TestEvent", "agg-1");
            StoredEvent saved = repository.save(event);
            entityManager.flush();
            Long id = saved.getId();

            // When
            repository.deleteById(id);
            entityManager.flush();
            entityManager.clear();

            // Then
            assertThat(repository.findById(id)).isEmpty();
        }
    }

    @Nested
    @DisplayName("自定义查询方法测试")
    class CustomQueryTests {

        @Test
        @DisplayName("根据eventId查询")
        void shouldFindByEventId() {
            // Given
            String eventId = "unique-event-" + UUID.randomUUID();
            StoredEvent event = createTestEvent(eventId, "TestEvent", null);
            repository.save(event);
            entityManager.flush();

            // When
            Optional<StoredEvent> found = repository.findByEventId(eventId);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("根据aggregateId查询")
        void shouldFindByAggregateId() {
            // Given
            String aggregateId = "order-123";
            repository.save(createTestEvent(null, "OrderCreated", aggregateId));
            repository.save(createTestEvent(null, "OrderUpdated", aggregateId));
            repository.save(createTestEvent(null, "OrderCreated", "order-456"));
            entityManager.flush();

            // When
            List<StoredEvent> events = repository.findByAggregateId(aggregateId);

            // Then
            assertThat(events).hasSize(2);
        }

        @Test
        @DisplayName("根据aggregateId按创建时间排序查询")
        void shouldFindByAggregateIdOrderByCreatedAt() {
            // Given
            String aggregateId = "timeline-test";
            StoredEvent event1 = createTestEvent(null, "Event1", aggregateId);
            event1.setCreatedAt(LocalDateTime.now().minusMinutes(10));
            StoredEvent event2 = createTestEvent(null, "Event2", aggregateId);
            event2.setCreatedAt(LocalDateTime.now().minusMinutes(5));
            StoredEvent event3 = createTestEvent(null, "Event3", aggregateId);
            event3.setCreatedAt(LocalDateTime.now());

            repository.save(event3);
            repository.save(event1);
            repository.save(event2);
            entityManager.flush();

            // When
            List<StoredEvent> events = repository.findByAggregateIdOrderByCreatedAt(aggregateId);

            // Then
            assertThat(events).hasSize(3);
            assertThat(events.get(0).getEventType()).isEqualTo("Event1");
            assertThat(events.get(1).getEventType()).isEqualTo("Event2");
            assertThat(events.get(2).getEventType()).isEqualTo("Event3");
        }

        @Test
        @DisplayName("根据eventType查询")
        void shouldFindByEventType() {
            // Given
            repository.save(createTestEvent(null, "UserCreated", null));
            repository.save(createTestEvent(null, "UserCreated", null));
            repository.save(createTestEvent(null, "OrderCreated", null));
            entityManager.flush();

            // When
            List<StoredEvent> userEvents = repository.findByEventType("UserCreated");
            List<StoredEvent> orderEvents = repository.findByEventType("OrderCreated");

            // Then
            assertThat(userEvents).hasSize(2);
            assertThat(orderEvents).hasSize(1);
        }

        @Test
        @DisplayName("根据时间范围查询")
        void shouldFindByOccurredOnBetween() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            StoredEvent event1 = createTestEvent(null, "Event1", null);
            event1.setOccurredOn(now.minusHours(2));
            StoredEvent event2 = createTestEvent(null, "Event2", null);
            event2.setOccurredOn(now.minusHours(1));
            StoredEvent event3 = createTestEvent(null, "Event3", null);
            event3.setOccurredOn(now.plusHours(2));

            repository.save(event1);
            repository.save(event2);
            repository.save(event3);
            entityManager.flush();

            // When
            List<StoredEvent> events = repository.findByOccurredOnBetween(
                    now.minusHours(3), now);

            // Then
            assertThat(events).hasSize(2);
        }

        @Test
        @DisplayName("查询未处理的事件")
        void shouldFindUnprocessedEvents() {
            // Given
            StoredEvent processed = createTestEvent(null, "Processed", null);
            processed.markAsProcessed();
            StoredEvent unprocessed1 = createTestEvent(null, "Unprocessed1", null);
            StoredEvent unprocessed2 = createTestEvent(null, "Unprocessed2", null);

            repository.save(processed);
            repository.save(unprocessed1);
            repository.save(unprocessed2);
            entityManager.flush();

            // When
            List<StoredEvent> events = repository.findByIsProcessedFalse();

            // Then
            assertThat(events).hasSize(2);
        }

        @Test
        @DisplayName("查询指定类型且未处理的事件")
        void shouldFindByEventTypeAndIsProcessedFalse() {
            // Given
            StoredEvent processed = createTestEvent(null, "TargetType", null);
            processed.markAsProcessed();
            StoredEvent unprocessed1 = createTestEvent(null, "TargetType", null);
            StoredEvent unprocessed2 = createTestEvent(null, "OtherType", null);

            repository.save(processed);
            repository.save(unprocessed1);
            repository.save(unprocessed2);
            entityManager.flush();

            // When
            List<StoredEvent> events = repository.findByEventTypeAndIsProcessedFalse("TargetType");

            // Then
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventType()).isEqualTo("TargetType");
        }

        @Test
        @DisplayName("检查事件是否存在")
        void shouldCheckIfEventExists() {
            // Given
            String eventId = "check-exists-" + UUID.randomUUID();
            StoredEvent event = createTestEvent(eventId, "TestEvent", null);
            repository.save(event);
            entityManager.flush();

            // When/Then
            assertThat(repository.existsByEventId(eventId)).isTrue();
            assertThat(repository.existsByEventId("non-existent")).isFalse();
        }
    }

    @Nested
    @DisplayName("分页查询测试")
    class PaginationTests {

        @Test
        @DisplayName("分页查询事件类型")
        void shouldPaginateByEventType() {
            // Given
            for (int i = 0; i < 25; i++) {
                repository.save(createTestEvent(null, "PaginatedEvent", null));
            }
            entityManager.flush();

            // When
            Page<StoredEvent> page1 = repository.findByEventType("PaginatedEvent", PageRequest.of(0, 10));
            Page<StoredEvent> page2 = repository.findByEventType("PaginatedEvent", PageRequest.of(1, 10));
            Page<StoredEvent> page3 = repository.findByEventType("PaginatedEvent", PageRequest.of(2, 10));

            // Then
            assertThat(page1.getContent()).hasSize(10);
            assertThat(page2.getContent()).hasSize(10);
            assertThat(page3.getContent()).hasSize(5);
            assertThat(page1.getTotalElements()).isEqualTo(25);
            assertThat(page1.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("分页查询未处理事件")
        void shouldPaginateUnprocessedEvents() {
            // Given
            for (int i = 0; i < 15; i++) {
                repository.save(createTestEvent(null, "UnprocessedEvent", null));
            }
            entityManager.flush();

            // When
            Page<StoredEvent> page = repository.findByIsProcessedFalse(PageRequest.of(0, 10));

            // Then
            assertThat(page.getContent()).hasSize(10);
            assertThat(page.getTotalElements()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("统计方法测试")
    class StatisticsTests {

        @Test
        @DisplayName("统计事件类型数量")
        void shouldCountByEventType() {
            // Given
            for (int i = 0; i < 5; i++) {
                repository.save(createTestEvent(null, "CountType", null));
            }
            for (int i = 0; i < 3; i++) {
                repository.save(createTestEvent(null, "OtherType", null));
            }
            entityManager.flush();

            // When
            long countTypeCount = repository.countByEventType("CountType");
            long otherTypeCount = repository.countByEventType("OtherType");

            // Then
            assertThat(countTypeCount).isEqualTo(5);
            assertThat(otherTypeCount).isEqualTo(3);
        }

        @Test
        @DisplayName("统计未处理事件数量")
        void shouldCountUnprocessedEvents() {
            // Given
            StoredEvent processed = createTestEvent(null, "Test", null);
            processed.markAsProcessed();
            repository.save(processed);

            for (int i = 0; i < 3; i++) {
                repository.save(createTestEvent(null, "Test", null));
            }
            entityManager.flush();

            // When
            long unprocessedCount = repository.countByIsProcessedFalse();

            // Then
            assertThat(unprocessedCount).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandlingTests {

        @Test
        @DisplayName("查询失败的事件（有错误消息）")
        void shouldFindFailedEvents() {
            // Given
            StoredEvent failedEvent = createTestEvent(null, "FailedEvent", null);
            failedEvent.markAsProcessingFailed("Connection timeout");
            repository.save(failedEvent);

            StoredEvent successEvent = createTestEvent(null, "SuccessEvent", null);
            successEvent.markAsProcessed();
            repository.save(successEvent);

            StoredEvent pendingEvent = createTestEvent(null, "PendingEvent", null);
            repository.save(pendingEvent);

            entityManager.flush();

            // When
            List<StoredEvent> failedEvents = repository.findByIsProcessedFalseAndErrorMessageIsNotNull();

            // Then
            assertThat(failedEvents).hasSize(1);
            assertThat(failedEvents.get(0).getErrorMessage()).isEqualTo("Connection timeout");
        }

        @Test
        @DisplayName("乐观锁版本控制")
        void shouldUpdateVersion() {
            // Given
            StoredEvent event = createTestEvent(null, "VersionTest", null);
            StoredEvent saved = repository.save(event);
            entityManager.flush();
            Long initialVersion = saved.getVersion();

            // When
            saved.markAsProcessed();
            repository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            StoredEvent updated = repository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getVersion()).isGreaterThan(initialVersion);
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("处理空聚合根ID - 应该返回aggregateId为null的事件")
        void shouldHandleNullAggregateId() {
            // Given
            StoredEvent eventWithNullAggregate = createTestEvent(null, "NoAggregateEvent", null);
            StoredEvent eventWithAggregate = createTestEvent(null, "WithAggregateEvent", "agg-123");
            repository.save(eventWithNullAggregate);
            repository.save(eventWithAggregate);
            entityManager.flush();
            entityManager.clear();

            // When - 查询aggregateId为null的事件
            List<StoredEvent> eventsWithNullAggregate = repository.findByAggregateId(null);
            List<StoredEvent> eventsWithAggregate = repository.findByAggregateId("agg-123");

            // Then - JPA会返回aggregateId为null的记录
            assertThat(eventsWithNullAggregate).hasSize(1);
            assertThat(eventsWithNullAggregate.get(0).getEventType()).isEqualTo("NoAggregateEvent");
            assertThat(eventsWithAggregate).hasSize(1);
            assertThat(eventsWithAggregate.get(0).getEventType()).isEqualTo("WithAggregateEvent");
        }

        @Test
        @DisplayName("处理大量事件")
        void shouldHandleLargeNumberOfEvents() {
            // Given
            int count = 100;
            for (int i = 0; i < count; i++) {
                repository.save(createTestEvent(null, "BulkEvent", "bulk-aggregate"));
            }
            entityManager.flush();

            // When
            long totalCount = repository.count();
            List<StoredEvent> aggregateEvents = repository.findByAggregateId("bulk-aggregate");

            // Then
            assertThat(totalCount).isEqualTo(count);
            assertThat(aggregateEvents).hasSize(count);
        }

        @Test
        @DisplayName("处理长事件数据")
        void shouldHandleLongEventData() {
            // Given
            String longData = "{\"data\": \"" + "x".repeat(10000) + "\"}";
            StoredEvent event = createTestEvent(null, "LongDataEvent", null);
            event.setEventData(longData);
            repository.save(event);
            entityManager.flush();
            entityManager.clear();

            // When
            StoredEvent loaded = repository.findByEventId(event.getEventId()).orElseThrow();

            // Then
            assertThat(loaded.getEventData()).isEqualTo(longData);
        }
    }
}
