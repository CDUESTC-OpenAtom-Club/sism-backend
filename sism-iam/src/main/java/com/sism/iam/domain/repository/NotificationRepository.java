package com.sism.iam.domain.repository;

import com.sism.iam.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * NotificationRepository - 通知仓储接口
 */
public interface NotificationRepository {

    Optional<Notification> findById(Long id);

    List<Notification> findAll();

    Page<Notification> findByRecipientUserId(Long recipientUserId, Pageable pageable);

    List<Notification> findByRecipientUserIdAndIsRead(Long recipientUserId, Boolean isRead);

    List<Notification> findByRecipientUserIdAndIsReadFalse(Long recipientUserId);

    long countByRecipientUserId(Long recipientUserId);

    long countByRecipientUserIdAndIsRead(Long recipientUserId, Boolean isRead);

    long countByRecipientUserIdAndPriority(Long recipientUserId, String priority);

    Notification save(Notification notification);

    void delete(Notification notification);

    Page<Notification> searchNotifications(Long userId, String keyword, Pageable pageable);
}
