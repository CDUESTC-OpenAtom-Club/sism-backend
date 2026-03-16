package com.sism.shared.infrastructure.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * EventStoreRepository - 事件存储库
 * 提供事件的数据库操作接口
 */
@Repository
public interface EventStoreRepository extends JpaRepository<StoredEvent, Long> {

    /**
     * 根据事件ID查找事件
     */
    Optional<StoredEvent> findByEventId(String eventId);

    /**
     * 根据聚合根ID查找所有事件
     */
    List<StoredEvent> findByAggregateId(String aggregateId);

    /**
     * 根据聚合根ID按创建时间排序查找事件
     */
    List<StoredEvent> findByAggregateIdOrderByCreatedAt(String aggregateId);

    /**
     * 根据事件类型查找事件
     */
    List<StoredEvent> findByEventType(String eventType);

    /**
     * 根据事件类型分页查找
     */
    Page<StoredEvent> findByEventType(String eventType, Pageable pageable);

    /**
     * 查找指定时间范围内的事件
     */
    List<StoredEvent> findByOccurredOnBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 查找有处理错误的事件
     */
    List<StoredEvent> findByIsProcessedFalseAndErrorMessageIsNotNull();

    /**
     * 查找未处理的事件
     */
    List<StoredEvent> findByIsProcessedFalse();

    /**
     * 分页查找未处理的事件
     */
    Page<StoredEvent> findByIsProcessedFalse(Pageable pageable);

    /**
     * 查找指定事件类型且未处理的事件
     */
    List<StoredEvent> findByEventTypeAndIsProcessedFalse(String eventType);

    /**
     * 统计事件总数
     */
    long countByEventType(String eventType);

    /**
     * 统计未处理的事件数量
     */
    long countByIsProcessedFalse();

    /**
     * 查找最新的事件
     */
    @Query("SELECT e FROM StoredEvent e WHERE e.aggregateId = :aggregateId ORDER BY e.createdAt DESC LIMIT 1")
    Optional<StoredEvent> findLatestByAggregateId(@Param("aggregateId") String aggregateId);

    /**
     * 查找在指定时间后发生的所有事件
     */
    List<StoredEvent> findByOccurredOnAfterOrderByOccurredOn(LocalDateTime after);

    /**
     * 检查事件是否存在
     */
    boolean existsByEventId(String eventId);
}
