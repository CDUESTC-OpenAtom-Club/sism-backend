package com.sism.shared.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sism.shared.domain.model.base.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EventStoreDatabaseTest - 数据库事件存储的单元测试
 *
 * 使用Mock进行单元测试，不需要真实数据库连接
 * 不影响任何业务表
 *
 * 测试覆盖:
 * - 事件保存（含幂等性）
 * - 事件查询
 * - 事件删除
 * - 统计信息
 * - 错误处理
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventStoreDatabase 单元测试")
class EventStoreDatabaseTest {

    @Mock
    private EventStoreRepository repository;

    private ObjectMapper objectMapper;
    private EventStoreDatabase eventStore;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        eventStore = new EventStoreDatabase(repository, objectMapper);
    }

    private StoredEvent createStoredEvent(TestDomainEvent event) {
        StoredEvent stored = new StoredEvent();
        stored.setId(1L);
        stored.setEventId(event.getEventId());
        stored.setEventType(event.getEventType());
        stored.setAggregateId(String.valueOf(event.getEntityId()));
        stored.setAggregateType("TestDomainEvent");
        try {
            stored.setEventData(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            stored.setEventData("{}");
        }
        stored.setOccurredOn(event.getOccurredOn());
        stored.setCreatedAt(LocalDateTime.now());
        stored.setIsProcessed(false);
        return stored;
    }

    @Nested
    @DisplayName("保存事件测试")
    class SaveEventTests {

        @Test
        @DisplayName("应该成功保存事件")
        void shouldSaveEvent() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test-data", 101L);
            when(repository.existsByEventId(event.getEventId())).thenReturn(false);
            when(repository.save(any(StoredEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            eventStore.save(event);

            // Then
            verify(repository).existsByEventId(event.getEventId());
            verify(repository).save(any(StoredEvent.class));
        }

        @Test
        @DisplayName("幂等性：重复事件不应再次保存")
        void shouldNotSaveDuplicateEvent() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test-data", 101L);
            when(repository.existsByEventId(event.getEventId())).thenReturn(true);

            // When
            eventStore.save(event);

            // Then
            verify(repository).existsByEventId(event.getEventId());
            verify(repository, never()).save(any(StoredEvent.class));
        }

        @Test
        @DisplayName("保存null事件应抛出异常")
        void shouldThrowExceptionForNullEvent() {
            assertThatThrownBy(() -> eventStore.save(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("序列化失败应抛出运行时异常")
        void shouldThrowExceptionOnSerializationFailure() {
            // Given - 创建一个无法序列化的事件
            DomainEvent badEvent = new DomainEvent() {
                @Override
                public String getEventType() {
                    return "BadEvent";
                }

                @Override
                public String getEventId() {
                    return "bad-event-id";
                }

                @Override
                public LocalDateTime getOccurredOn() {
                    return LocalDateTime.now();
                }

                // 添加一个会导致序列化失败的循环引用
                public Object getSelf() {
                    return this;
                }
            };

            when(repository.existsByEventId(anyString())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> eventStore.save(badEvent))
                    .isInstanceOf(RuntimeException.class);
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
            StoredEvent stored = createStoredEvent(event);
            when(repository.findByEventId(event.getEventId())).thenReturn(Optional.of(stored));

            // When
            Optional<DomainEvent> found = eventStore.findById(event.getEventId());

            // Then
            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("查询null ID应返回空")
        void shouldReturnEmptyForNullId() {
            Optional<DomainEvent> result = eventStore.findById(null);
            assertThat(result).isEmpty();
            verify(repository, never()).findByEventId(any());
        }

        @Test
        @DisplayName("查询空字符串ID应返回空")
        void shouldReturnEmptyForEmptyId() {
            Optional<DomainEvent> result = eventStore.findById("");
            assertThat(result).isEmpty();
            verify(repository, never()).findByEventId(any());
        }

        @Test
        @DisplayName("根据聚合根ID查询事件")
        void shouldFindEventsByAggregateId() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 100L);
            StoredEvent stored = createStoredEvent(event);
            when(repository.findByAggregateIdOrderByCreatedAt("100"))
                    .thenReturn(Collections.singletonList(stored));

            // When
            List<DomainEvent> events = eventStore.findByAggregateId("100");

            // Then
            assertThat(events).hasSize(1);
        }

        @Test
        @DisplayName("查询null聚合根ID应返回空列表")
        void shouldReturnEmptyListForNullAggregateId() {
            List<DomainEvent> events = eventStore.findByAggregateId(null);
            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("根据事件类型查询事件")
        void shouldFindEventsByEventType() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 100L);
            StoredEvent stored = createStoredEvent(event);
            when(repository.findByEventType("TestDomainEvent"))
                    .thenReturn(Collections.singletonList(stored));

            // When
            List<DomainEvent> events = eventStore.findByEventType("TestDomainEvent");

            // Then
            assertThat(events).hasSize(1);
        }

        @Test
        @DisplayName("根据时间范围查询事件")
        void shouldFindEventsByTimeRange() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(1);
            LocalDateTime end = LocalDateTime.now().plusHours(1);
            TestDomainEvent event = new TestDomainEvent("test", 100L);
            StoredEvent stored = createStoredEvent(event);
            when(repository.findByOccurredOnBetween(start, end))
                    .thenReturn(Collections.singletonList(stored));

            // When
            List<DomainEvent> events = eventStore.findByTimeRange(start, end);

            // Then
            assertThat(events).hasSize(1);
        }

        @Test
        @DisplayName("无效时间范围应返回空列表")
        void shouldReturnEmptyListForInvalidTimeRange() {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = LocalDateTime.now().minusHours(1); // end < start

            List<DomainEvent> events = eventStore.findByTimeRange(start, end);

            assertThat(events).isEmpty();
            verify(repository, never()).findByOccurredOnBetween(any(), any());
        }
    }

    @Nested
    @DisplayName("删除事件测试")
    class DeleteEventTests {

        @Test
        @DisplayName("应该成功删除单个事件")
        void shouldDeleteEvent() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 100L);
            StoredEvent stored = createStoredEvent(event);
            when(repository.findByEventId(event.getEventId())).thenReturn(Optional.of(stored));

            // When
            eventStore.delete(event.getEventId());

            // Then
            verify(repository).delete(stored);
        }

        @Test
        @DisplayName("删除null事件ID不应调用repository")
        void shouldNotDeleteForNullId() {
            eventStore.delete(null);
            verify(repository, never()).findByEventId(any());
        }

        @Test
        @DisplayName("删除空字符串事件ID不应调用repository")
        void shouldNotDeleteForEmptyId() {
            eventStore.delete("");
            verify(repository, never()).findByEventId(any());
        }

        @Test
        @DisplayName("应该成功根据聚合根ID删除所有事件")
        void shouldDeleteByAggregateId() {
            // Given
            String aggregateId = "100";
            List<StoredEvent> events = Arrays.asList(
                    createStoredEvent(new TestDomainEvent("test1", 100L)),
                    createStoredEvent(new TestDomainEvent("test2", 100L))
            );
            when(repository.findByAggregateId(aggregateId)).thenReturn(events);

            // When
            eventStore.deleteByAggregateId(aggregateId);

            // Then
            verify(repository).deleteAll(events);
        }
    }

    @Nested
    @DisplayName("统计信息测试")
    class StatisticsTests {

        @Test
        @DisplayName("应该返回正确的统计信息")
        void shouldReturnCorrectStatistics() {
            // Given
            when(repository.count()).thenReturn(100L);
            when(repository.countByIsProcessedFalse()).thenReturn(20L);

            // When
            Map<String, Object> stats = eventStore.getStatistics();

            // Then
            assertThat(stats)
                    .containsEntry("totalEvents", 100L)
                    .containsEntry("unprocessedEvents", 20L)
                    .containsKey("successRate");

            String successRate = (String) stats.get("successRate");
            assertThat(successRate).isEqualTo("80.00%");
        }

        @Test
        @DisplayName("空存储应该返回0%成功率")
        void shouldHandleEmptyStore() {
            // Given
            when(repository.count()).thenReturn(0L);

            // When
            Map<String, Object> stats = eventStore.getStatistics();

            // Then
            assertThat(stats)
                    .containsEntry("totalEvents", 0L);
            // 当total为0时，不应包含successRate
            assertThat(stats.containsKey("successRate")).isFalse();
        }
    }

    @Nested
    @DisplayName("事件处理标记测试")
    class ProcessingMarkTests {

        @Test
        @DisplayName("应该正确标记事件为已处理")
        void shouldMarkEventAsProcessed() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 100L);
            StoredEvent stored = createStoredEvent(event);
            when(repository.findByEventId(event.getEventId())).thenReturn(Optional.of(stored));
            when(repository.save(any(StoredEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            eventStore.markEventAsProcessed(event.getEventId());

            // Then
            verify(repository).save(argThat(e ->
                    e.getIsProcessed() && e.getProcessedAt() != null
            ));
        }

        @Test
        @DisplayName("应该能查询未处理的事件")
        void shouldFindUnprocessedEvents() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 100L);
            StoredEvent stored = createStoredEvent(event);
            when(repository.findByIsProcessedFalse()).thenReturn(Collections.singletonList(stored));

            // When
            List<DomainEvent> unprocessed = eventStore.findUnprocessedEvents();

            // Then
            assertThat(unprocessed).hasSize(1);
        }

        @Test
        @DisplayName("应该能查询指定类型的未处理事件")
        void shouldFindUnprocessedEventsByType() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 100L);
            StoredEvent stored = createStoredEvent(event);
            when(repository.findByEventTypeAndIsProcessedFalse("TestDomainEvent"))
                    .thenReturn(Collections.singletonList(stored));

            // When
            List<DomainEvent> events = eventStore.findUnprocessedEventsByType("TestDomainEvent");

            // Then
            assertThat(events).hasSize(1);
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("数据库保存失败应抛出运行时异常")
        void shouldThrowExceptionOnDatabaseSaveFailure() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 100L);
            when(repository.existsByEventId(event.getEventId())).thenReturn(false);
            when(repository.save(any(StoredEvent.class))).thenThrow(new RuntimeException("DB error"));

            // When/Then
            assertThatThrownBy(() -> eventStore.save(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to save event");
        }

        @Test
        @DisplayName("查询异常应返回空结果而不是抛出异常")
        void shouldReturnEmptyOnQueryException() {
            // Given
            when(repository.findByEventId(anyString())).thenThrow(new RuntimeException("Query failed"));

            // When
            Optional<DomainEvent> result = eventStore.findById("any-id");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("删除异常应抛出运行时异常")
        void shouldThrowExceptionOnDeleteFailure() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 100L);
            StoredEvent stored = createStoredEvent(event);
            when(repository.findByEventId(event.getEventId())).thenReturn(Optional.of(stored));
            doThrow(new RuntimeException("Delete failed")).when(repository).delete(stored);

            // When/Then
            assertThatThrownBy(() -> eventStore.delete(event.getEventId()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to delete event");
        }
    }
}
