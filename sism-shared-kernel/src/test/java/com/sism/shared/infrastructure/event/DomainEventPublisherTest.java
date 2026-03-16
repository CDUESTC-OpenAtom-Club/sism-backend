package com.sism.shared.infrastructure.event;

import com.sism.shared.domain.model.base.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DomainEventPublisherTest - 领域事件发布器的完整单元测试
 *
 * 测试覆盖:
 * - 单事件发布
 * - 批量事件发布
 * - 错误处理
 * - 与EventStore和ApplicationEventPublisher的交互
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DomainEventPublisher 单元测试")
class DomainEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private EventStore eventStore;

    private DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUp() {
        domainEventPublisher = new DomainEventPublisher(applicationEventPublisher, eventStore);
    }

    @Nested
    @DisplayName("单事件发布测试")
    class SingleEventPublishTests {

        @Test
        @DisplayName("应该成功发布单个事件")
        void shouldPublishSingleEvent() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test-data", 1L);

            // When
            domainEventPublisher.publish(event);

            // Then
            verify(eventStore, times(1)).save(event);
            verify(applicationEventPublisher, times(1)).publishEvent(event);
        }

        @Test
        @DisplayName("发布null事件不应调用任何方法")
        void shouldNotPublishNullEvent() {
            // When
            domainEventPublisher.publish(null);

            // Then
            verify(eventStore, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("事件存储失败应该抛出运行时异常")
        void shouldThrowExceptionWhenStoreFails() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test-data", 1L);
            doThrow(new RuntimeException("Storage error")).when(eventStore).save(event);

            // When/Then
            assertThatThrownBy(() -> domainEventPublisher.publish(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to publish domain event");
        }

        @Test
        @DisplayName("应用事件发布失败应该抛出运行时异常")
        void shouldThrowExceptionWhenApplicationPublishFails() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test-data", 1L);
            doThrow(new RuntimeException("Publish error")).when(applicationEventPublisher).publishEvent(event);

            // When/Then
            assertThatThrownBy(() -> domainEventPublisher.publish(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to publish domain event");
        }
    }

    @Nested
    @DisplayName("批量事件发布测试")
    class BatchEventPublishTests {

        @Test
        @DisplayName("应该成功发布多个事件")
        void shouldPublishMultipleEvents() {
            // Given
            TestDomainEvent event1 = new TestDomainEvent("data1", 1L);
            TestDomainEvent event2 = new TestDomainEvent("data2", 2L);
            TestDomainEvent event3 = new TestDomainEvent("data3", 3L);
            List<DomainEvent> events = Arrays.asList(event1, event2, event3);

            // When
            domainEventPublisher.publishAll(events);

            // Then
            verify(eventStore, times(3)).save(any(DomainEvent.class));
            verify(applicationEventPublisher, times(3)).publishEvent(any(DomainEvent.class));
        }

        @Test
        @DisplayName("发布空列表不应调用任何方法")
        void shouldNotPublishEmptyList() {
            // When
            domainEventPublisher.publishAll(Collections.emptyList());

            // Then
            verify(eventStore, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("发布null列表不应调用任何方法")
        void shouldNotPublishNullList() {
            // When
            domainEventPublisher.publishAll(null);

            // Then
            verify(eventStore, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("部分事件失败时应继续发布其他事件")
        void shouldContinuePublishingWhenSomeEventsFail() {
            // Given
            TestDomainEvent event1 = new TestDomainEvent("data1", 1L);
            TestDomainEvent event2 = new TestDomainEvent("data2", 2L);
            TestDomainEvent event3 = new TestDomainEvent("data3", 3L);
            List<DomainEvent> events = Arrays.asList(event1, event2, event3);

            // 第二个事件存储失败
            doNothing().when(eventStore).save(event1);
            doThrow(new RuntimeException("Storage error")).when(eventStore).save(event2);
            doNothing().when(eventStore).save(event3);

            // When
            domainEventPublisher.publishAll(events);

            // Then - 应该尝试保存所有3个事件
            verify(eventStore, times(3)).save(any(DomainEvent.class));
            // 第1和第3个事件应该被发布到ApplicationEventPublisher
            verify(applicationEventPublisher, times(1)).publishEvent(event1);
            verify(applicationEventPublisher, never()).publishEvent(event2); // 第2个失败了
            verify(applicationEventPublisher, times(1)).publishEvent(event3);
        }
    }

    @Nested
    @DisplayName("事件类型测试")
    class EventTypeTests {

        @Test
        @DisplayName("应该能发布不同类型的事件")
        void shouldPublishDifferentEventTypes() {
            // Given
            TestDomainEvent domainEvent = new TestDomainEvent("data", 1L);
            TestAggregateEvent aggregateEvent = new TestAggregateEvent("agg-123", "CREATE", null);

            // When
            domainEventPublisher.publish(domainEvent);
            domainEventPublisher.publish(aggregateEvent);

            // Then
            verify(eventStore).save(domainEvent);
            verify(eventStore).save(aggregateEvent);
            verify(applicationEventPublisher).publishEvent(domainEvent);
            verify(applicationEventPublisher).publishEvent(aggregateEvent);
        }
    }

    @Nested
    @DisplayName("顺序保证测试")
    class OrderingTests {

        @Test
        @DisplayName("应该先存储事件再发布")
        void shouldStoreBeforePublish() {
            // Given
            TestDomainEvent event = new TestDomainEvent("test", 1L);

            // When
            domainEventPublisher.publish(event);

            // Then - 使用 InOrder 验证调用顺序
            var inOrder = inOrder(eventStore, applicationEventPublisher);
            inOrder.verify(eventStore).save(event);
            inOrder.verify(applicationEventPublisher).publishEvent(event);
        }
    }
}
