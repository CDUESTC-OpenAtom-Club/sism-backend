package com.sism.shared.infrastructure.event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * StoredEvent - 持久化的事件记录
 * 用于在数据库中存储领域事件，支持事件溯源
 */
@Entity
@Table(name = "event_store", indexes = {
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_occurred_on", columnList = "occurred_on"),
    @Index(name = "idx_processed", columnList = "is_processed")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoredEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_id", unique = true, nullable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_id")
    private String aggregateId;

    @Column(name = "aggregate_type")
    private String aggregateType;

    @Column(name = "event_data", columnDefinition = "TEXT", nullable = false)
    private String eventData;

    @Column(name = "occurred_on", nullable = false)
    private LocalDateTime occurredOn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * 标记事件已被处理
     */
    public void markAsProcessed() {
        this.isProcessed = true;
        this.processedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    /**
     * 标记事件处理失败
     */
    public void markAsProcessingFailed(String error) {
        this.isProcessed = false;
        this.errorMessage = error;
    }

    /**
     * 获取 payload (兼容旧代码)
     */
    public String getPayload() {
        return this.eventData;
    }
}
