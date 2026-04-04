package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.UserNotification;
import com.sism.iam.domain.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaUserNotificationRepository implements UserNotificationRepository {

    private final JpaUserNotificationRepositoryInternal jpaRepository;

    @Override
    public Page<UserNotification> findByRecipientUserId(Long recipientUserId, Pageable pageable) {
        return jpaRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId, pageable);
    }

    @Override
    public Page<UserNotification> findByRecipientUserIdAndStatus(Long recipientUserId, String status, Pageable pageable) {
        return jpaRepository.findByRecipientUserIdAndStatusOrderByCreatedAtDesc(recipientUserId, status, pageable);
    }

    @Override
    public Optional<UserNotification> findByIdAndRecipientUserId(Long id, Long recipientUserId) {
        return jpaRepository.findByIdAndRecipientUserId(id, recipientUserId);
    }

    @Override
    public List<UserNotification> saveAll(List<UserNotification> notifications) {
        return jpaRepository.saveAll(notifications);
    }

    @Override
    public UserNotification save(UserNotification notification) {
        return jpaRepository.save(notification);
    }

    @Override
    public long markAllAsRead(Long recipientUserId) {
        return jpaRepository.markAllAsRead(recipientUserId, LocalDateTime.now());
    }

    @Override
    public Optional<UserNotification> findLatestReminder(Long relatedEntityId, Long senderUserId) {
        return jpaRepository
                .findTopByNotificationTypeAndRelatedEntityTypeAndRelatedEntityIdAndSenderUserIdOrderByCreatedAtDesc(
                        "REMINDER",
                        "INDICATOR",
                        relatedEntityId,
                        senderUserId
                );
    }

    @Override
    public List<UserNotification> findLatestReminders(Collection<Long> relatedEntityIds, Long senderUserId) {
        List<UserNotification> notifications = jpaRepository
                .findByNotificationTypeAndRelatedEntityTypeAndRelatedEntityIdInAndSenderUserIdOrderByCreatedAtDesc(
                        "REMINDER",
                        "INDICATOR",
                        relatedEntityIds,
                        senderUserId
                );
        Map<Long, UserNotification> latestByEntityId = new LinkedHashMap<>();
        for (UserNotification notification : notifications) {
            latestByEntityId.putIfAbsent(notification.getRelatedEntityId(), notification);
        }
        return List.copyOf(latestByEntityId.values());
    }

    @Override
    public long countReminderBatches(Long relatedEntityId, Long senderUserId) {
        return jpaRepository.countDistinctBatchKeyByReminder("REMINDER", "INDICATOR", relatedEntityId, senderUserId);
    }
}
