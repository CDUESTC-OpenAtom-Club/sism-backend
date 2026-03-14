package com.sism.shared.domain.model.base;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件接口
 * DDD中领域事件表示领域中发生的事情
 */
public interface DomainEvent {

    /**
     * 获取事件ID
     */
    default String getEventId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 获取事件发生时间
     */
    default LocalDateTime getOccurredOn() {
        return LocalDateTime.now();
    }

    /**
     * 获取事件类型
     */
    String getEventType();
}
