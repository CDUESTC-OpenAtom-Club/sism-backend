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
@Table(name = "notification", schema = "public")
public class Notification extends AggregateRoot<Long> {

    @Id
    @SequenceGenerator(name = "Notification_IdSeq", sequenceName = "public.notification_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Notification_IdSeq")
    @Column(name = "id")
    private Long id;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "related_indicator_id")
    private Long relatedIndicatorId;

    @Column(name = "related_report_id")
    private Long relatedReportId;

    @Column(name = "related_task_id")
    private Long relatedTaskId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "sent_at", nullable = false)
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
}
