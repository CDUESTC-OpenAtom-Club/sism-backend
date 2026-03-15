package com.sism.iam.domain;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Notification - 通知实体
 */
@Getter
@Setter
@Entity
@Table(name = "alert_event")
public class Notification extends AggregateRoot<Long> {

    @Id
    @SequenceGenerator(name = "Notification_IdSeq", sequenceName = "public.notification_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Notification_IdSeq")
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "event_type", nullable = false)
    private String notificationType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String message;

    @Column(name = "indicator_id")
    private Long relatedIndicatorId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_time")
    private LocalDateTime readAt;

    @Transient
    private LocalDateTime sentAt;

    @Column(name = "priority")
    private String priority = "NORMAL";

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Notification create(Long recipientUserId, String notificationType, String title, String message) {
        Notification notification = new Notification();
        notification.recipientUserId = recipientUserId;
        notification.notificationType = notificationType;
        notification.title = title;
        notification.message = message;
        notification.isRead = false;
        notification.priority = "NORMAL";
        notification.isDeleted = false;
        notification.sentAt = LocalDateTime.now();
        notification.createdAt = LocalDateTime.now();
        notification.updatedAt = LocalDateTime.now();
        return notification;
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isHighPriority() {
        return "HIGH".equals(priority) || "URGENT".equals(priority);
    }

    @PrePersist
    protected void onCreate() {
        if (isRead == null) {
            isRead = false;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }

    @Override
    public void validate() {
        // 通知验证逻辑
        if (recipientUserId == null) {
            throw new IllegalArgumentException("Recipient user ID is required");
        }
        if (notificationType == null || notificationType.isBlank()) {
            throw new IllegalArgumentException("Notification type is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Notification title is required");
        }
        if (sentAt == null) {
            throw new IllegalArgumentException("Notification sent time is required");
        }
    }
}
