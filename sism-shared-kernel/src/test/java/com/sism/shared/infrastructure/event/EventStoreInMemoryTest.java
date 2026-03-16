package com.sism.shared.infrastructure.event;

import com.sism.shared.domain.model.base.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * EventStoreInMemoryTest - 内存事件存储的完整单元测试
 *
 * 测试覆盖:
 * - 基本CRUD操作
 * - 查询功能
 * - 聚合根事件管理
 * - 并发安全
 * - 边界条件
 * - 统计功能
 */
@DisplayName("EventStoreInMemory 单元测试")
class EventStoreInMemoryTest {

    private EventStoreInMemory eventStore;

    @BeforeEach
    void setUp() {
        eventStore = new EventStoreInMemory();
    }

    @Nested
    @DisplayName("保存事件测试")
    class SaveEventTests {

        @Test
        @DisplayName("应该成功保存普通领域事件")
        void shouldSaveNormalDomainEvent() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test-data", 1L);

            // When
            eventStore.save(event);

            // Then
            assertThat(eventStore.count()).isEqualTo(1);
            assertThat(eventStore.findById(event.getEventId()))
                    .isPresent()
                    .get()
                    .extracting(DomainEvent::getEventType)
                    .isEqualTo("TestDomainEvent");
        }

        @Test
        @DisplayName("应该成功保存带聚合根ID的事件")
        void shouldSaveAggregateEvent() {
            // Given
            TestAggregateEvent event = new TestAggregateEvent("aggregate-123", "CREATE", "payload");

            // When
            eventStore.save(event);

            // Then
            List<DomainEvent> aggregateEvents = eventStore.findByAggregateId("aggregate-123");
            assertThat(aggregateEvents)
                    .hasSize(1)
                    .first()
                    .extracting(DomainEvent::getEventId)
                    .isEqualTo(event.getEventId());
        }

        @Test
        @DisplayName("保存null事件应该抛出异常")
        void shouldThrowExceptionWhenSavingNull() {
            assertThatThrownBy(() -> eventStore.save(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("应该能保存多个不同事件")
        void shouldSaveMultipleEvents() {
            // Given
            TestDomainEvent event1 = new TestDomainEvent("data1", 1L);
            TestDomainEvent event2 = new TestDomainEvent("data2", 2L);
            TestAggregateEvent event3 = new TestAggregateEvent("agg-1", "UPDATE", null);

            // When
            eventStore.save(event1);
            eventStore.save(event2);
            eventStore.save(event3);

            // Then
            assertThat(eventStore.count()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("查询事件测试")
    class FindEventTests {

        @Test
        @DisplayName("根据ID查询存在的事件")
        void shouldFindEventById() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 100L);
            eventStore.save(event);

            // When
            Optional<DomainEvent> found = eventStore.findById(event.getEventId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getEventId()).isEqualTo(event.getEventId());
        }

        @Test
        @DisplayName("根据不存在的ID查询应返回空")
        void shouldReturnEmptyForNonExistentId() {
            Optional<DomainEvent> found = eventStore.findById("non-existent-id");
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("根据聚合根ID查询所有相关事件")
        void shouldFindAllEventsByAggregateId() {
            // Given
            String aggregateId = "order-123";
            TestAggregateEvent event1 = new TestAggregateEvent(aggregateId, "CREATED", null);
            TestAggregateEvent event2 = new TestAggregateEvent(aggregateId, "UPDATED", null);
            TestAggregateEvent event3 = new TestAggregateEvent("other-aggregate", "CREATED", null);
            eventStore.save(event1);
            eventStore.save(event2);
            eventStore.save(event3);

            // When
            List<DomainEvent> events = eventStore.findByAggregateId(aggregateId);

            // Then
            assertThat(events).hasSize(2);
        }

        @Test
        @DisplayName("根据事件类型查询事件")
        void shouldFindEventsByType() {
            // Given
            TestDomainEvent event1 = new TestDomainEvent("data1", 1L);
            TestDomainEvent event2 = new TestDomainEvent("data2", 2L);
            TestAggregateEvent event3 = new TestAggregateEvent("agg", "action", null);
            eventStore.save(event1);
            eventStore.save(event2);
            eventStore.save(event3);

            // When
            List<DomainEvent> testEvents = eventStore.findByEventType("TestDomainEvent");
            List<DomainEvent> aggregateEvents = eventStore.findByEventType("TestAggregateEvent");

            // Then
            assertThat(testEvents).hasSize(2);
            assertThat(aggregateEvents).hasSize(1);
        }

        @Test
        @DisplayName("根据时间范围查询事件")
        void shouldFindEventsByTimeRange() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 1L);
            eventStore.save(event);

            LocalDateTime start = LocalDateTime.now().minusMinutes(1);
            LocalDateTime end = LocalDateTime.now().plusMinutes(1);

            // When
            List<DomainEvent> events = eventStore.findByTimeRange(start, end);

            // Then
            assertThat(events).hasSize(1);
        }

        @Test
        @DisplayName("时间范围之外的事件不应被返回")
        void shouldNotReturnEventsOutsideTimeRange() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 1L);
            eventStore.save(event);

            LocalDateTime start = LocalDateTime.now().plusHours(1);
            LocalDateTime end = LocalDateTime.now().plusHours(2);

            // When
            List<DomainEvent> events = eventStore.findByTimeRange(start, end);

            // Then
            assertThat(events).isEmpty();
        }
    }

    @Nested
    @DisplayName("删除事件测试")
    class DeleteEventTests {

        @Test
        @DisplayName("应该能删除单个事件")
        void shouldDeleteSingleEvent() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 1L);
            eventStore.save(event);
            assertThat(eventStore.count()).isEqualTo(1);

            // When
            eventStore.delete(event.getEventId());

            // Then
            assertThat(eventStore.count()).isEqualTo(0);
            assertThat(eventStore.findById(event.getEventId())).isEmpty();
        }

        @Test
        @DisplayName("删除不存在的事件不应报错")
        void shouldNotThrowWhenDeletingNonExistent() {
            assertThatCode(() -> eventStore.delete("non-existent"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应该能删除聚合根的所有事件")
        void shouldDeleteAllEventsByAggregateId() {
            // Given
            String aggregateId = "agg-to-delete";
            eventStore.save(new TestAggregateEvent(aggregateId, "CREATE", null));
            eventStore.save(new TestAggregateEvent(aggregateId, "UPDATE", null));
            eventStore.save(new TestAggregateEvent("other-agg", "CREATE", null));
            assertThat(eventStore.count()).isEqualTo(3);

            // When
            eventStore.deleteByAggregateId(aggregateId);

            // Then
            assertThat(eventStore.count()).isEqualTo(1);
            assertThat(eventStore.findByAggregateId(aggregateId)).isEmpty();
        }

        @Test
        @DisplayName("删除聚合事件后，按ID也应查询不到")
        void shouldRemoveFromIdMapWhenDeletingAggregateEvent() {
            // Given
            TestAggregateEvent event = new TestAggregateEvent("agg-123", "CREATE", null);
            eventStore.save(event);

            // When
            eventStore.delete(event.getEventId());

            // Then
            assertThat(eventStore.findById(event.getEventId())).isEmpty();
            assertThat(eventStore.findByAggregateId("agg-123")).isEmpty();
        }
    }

    @Nested
    @DisplayName("清理和统计测试")
    class ClearAndStatisticsTests {

        @Test
        @DisplayName("clear()应该清除所有事件")
        void shouldClearAllEvents() {
            // Given
            eventStore.save(new TestDomainEvent("data1", 1L));
            eventStore.save(new TestDomainEvent("data2", 2L));
            eventStore.save(new TestAggregateEvent("agg", "action", null));
            assertThat(eventStore.count()).isEqualTo(3);

            // When
            eventStore.clear();

            // Then
            assertThat(eventStore.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("统计信息应该正确反映事件类型分布")
        void shouldReturnCorrectStatistics() {
            // Given
            eventStore.save(new TestDomainEvent("data1", 1L));
            eventStore.save(new TestDomainEvent("data2", 2L));
            eventStore.save(new TestDomainEvent("data3", 3L));
            eventStore.save(new TestAggregateEvent("agg", "action", null));

            // When
            Map<String, Long> stats = eventStore.getStatistics();

            // Then
            assertThat(stats)
                    .containsEntry("TestDomainEvent", 3L)
                    .containsEntry("TestAggregateEvent", 1L);
        }

        @Test
        @DisplayName("空存储的统计信息应为空")
        void shouldReturnEmptyStatisticsForEmptyStore() {
            Map<String, Long> stats = eventStore.getStatistics();
            assertThat(stats).isEmpty();
        }
    }

    @Nested
    @DisplayName("并发安全测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("多线程并发保存事件应该是安全的")
        void shouldBeSafeForConcurrentSaves() throws InterruptedException {
            // Given
            int threadCount = 10;
            int eventsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // When
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < eventsPerThread; j++) {
                            eventStore.save(new TestDomainEvent("data-" + j, (long) j));
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            // Then
            assertThat(eventStore.count()).isEqualTo(threadCount * eventsPerThread);
        }

        @Test
        @DisplayName("并发读写应该不会导致异常")
        void shouldHandleConcurrentReadAndWrite() throws InterruptedException {
            // Given
            int operations = 1000;
            ExecutorService executor = Executors.newFixedThreadPool(4);
            CountDownLatch latch = new CountDownLatch(4);

            // When - 2个线程写入，2个线程读取
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operations; i++) {
                        eventStore.save(new TestDomainEvent("write1-" + i, (long) i));
                    }
                } finally {
                    latch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    for (int i = 0; i < operations; i++) {
                        eventStore.save(new TestAggregateEvent("agg-" + i, "action", null));
                    }
                } finally {
                    latch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    for (int i = 0; i < operations; i++) {
                        eventStore.findByEventType("TestDomainEvent");
                        eventStore.count();
                    }
                } finally {
                    latch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    for (int i = 0; i < operations; i++) {
                        eventStore.getStatistics();
                        eventStore.findByAggregateId("agg-" + (i % 100));
                    }
                } finally {
                    latch.countDown();
                }
            });

            boolean completed = latch.await(60, TimeUnit.SECONDS);
            executor.shutdown();

            // Then
            assertThat(completed).isTrue();
            assertThat(eventStore.count()).isEqualTo(operations * 2);
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("查询空存储应返回空列表而非null")
        void shouldReturnEmptyListNotNull() {
            assertThat(eventStore.findByAggregateId("any")).isEmpty();
            assertThat(eventStore.findByEventType("any")).isEmpty();
            assertThat(eventStore.findByTimeRange(LocalDateTime.now(), LocalDateTime.now())).isEmpty();
        }

        @Test
        @DisplayName("时间范围边界值应该被正确包含")
        void shouldIncludeBoundaryTimestamps() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 1L);
            eventStore.save(event);

            // When - 使用精确的时间范围
            LocalDateTime exactTime = event.getOccurredOn();
            List<DomainEvent> events = eventStore.findByTimeRange(exactTime, exactTime);

            // Then
            assertThat(events).hasSize(1);
        }

        @Test
        @DisplayName("处理超长事件ID")
        void shouldHandleLongEventId() {
            // Given
            String longId = "event-" + "x".repeat(500);
            TestDomainEvent event = new TestDomainEvent(longId, longId, LocalDateTime.now(), "test", 1L);

            // When
            eventStore.save(event);

            // Then
            assertThat(eventStore.findById(longId)).isPresent();
        }

        @Test
        @DisplayName("处理特殊字符的事件数据")
        void shouldHandleSpecialCharacters() {
            // Given
            TestDomainEvent event = new TestDomainEvent("特殊字符: @#$%^&*()_+={}[]|\\:\";<>?,./~`", 1L);

            // When
            eventStore.save(event);

            // Then
            assertThat(eventStore.count()).isEqualTo(1);
            Optional<DomainEvent> found = eventStore.findById(event.getEventId());
            assertThat(found).isPresent();
        }
    }
}
