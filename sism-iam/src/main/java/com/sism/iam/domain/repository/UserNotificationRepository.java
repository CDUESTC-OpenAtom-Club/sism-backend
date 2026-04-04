package com.sism.iam.domain.repository;

import com.sism.iam.domain.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface UserNotificationRepository {

    Page<UserNotification> findByRecipientUserId(Long recipientUserId, Pageable pageable);

    Page<UserNotification> findByRecipientUserIdAndStatus(Long recipientUserId, String status, Pageable pageable);

    Optional<UserNotification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    List<UserNotification> saveAll(List<UserNotification> notifications);

    UserNotification save(UserNotification notification);

    int markAsRead(Long id, Long recipientUserId, LocalDateTime readAt);

    long markAllAsRead(Long recipientUserId);

    Optional<UserNotification> findLatestReminder(Long relatedEntityId, Long senderUserId);

    List<UserNotification> findLatestReminders(Collection<Long> relatedEntityIds, Long senderUserId);

    long countReminderBatches(Long relatedEntityId, Long senderUserId);
}
