package com.sism.shared.infrastructure.event;

import com.sism.shared.domain.model.base.DomainEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 事件存储接口
 * 用于持久化领域事件
 */
public interface EventStore {

    /**
     * 保存事件
     */
    void save(DomainEvent event);

    /**
     * 根据事件ID查询事件
     */
    Optional<DomainEvent> findById(String eventId);

    /**
     * 根据聚合根ID查询事件
     */
    List<DomainEvent> findByAggregateId(String aggregateId);

    /**
     * 根据事件类型查询事件
     */
    List<DomainEvent> findByEventType(String eventType);

    /**
     * 查询指定时间范围内的事件
     */
    List<DomainEvent> findByTimeRange(LocalDateTime start, LocalDateTime end);

    /**
     * 删除事件（通常用于测试）
     */
    void delete(String eventId);

    /**
     * 删除聚合根的所有事件
     */
    void deleteByAggregateId(String aggregateId);
}
