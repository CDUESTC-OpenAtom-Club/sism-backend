package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.UserNotification;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaUserNotificationRepositoryInternal extends JpaRepository<UserNotification, Long> {

    Page<UserNotification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    Page<UserNotification> findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
            Long recipientUserId,
            String status,
            Pageable pageable
    );

    Optional<UserNotification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    Optional<UserNotification> findTopByNotificationTypeAndRelatedEntityTypeAndRelatedEntityIdAndSenderUserIdOrderByCreatedAtDesc(
            String notificationType,
            String relatedEntityType,
            Long relatedEntityId,
            Long senderUserId
    );

    List<UserNotification> findByNotificationTypeAndRelatedEntityTypeAndRelatedEntityIdInAndSenderUserIdOrderByCreatedAtDesc(
            String notificationType,
            String relatedEntityType,
            Collection<Long> relatedEntityIds,
            Long senderUserId
    );

    @Query("""
            select count(distinct n.batchKey)
              from UserNotification n
             where n.notificationType = :notificationType
               and n.relatedEntityType = :relatedEntityType
               and n.relatedEntityId = :relatedEntityId
               and n.senderUserId = :senderUserId
            """)
    long countDistinctBatchKeyByReminder(
            @Param("notificationType") String notificationType,
            @Param("relatedEntityType") String relatedEntityType,
            @Param("relatedEntityId") Long relatedEntityId,
            @Param("senderUserId") Long senderUserId
    );

    @Modifying
    @Transactional
    @Query("""
            update UserNotification n
               set n.status = 'READ',
                   n.readAt = :readAt,
                   n.updatedAt = :readAt
             where n.recipientUserId = :recipientUserId
               and n.status <> 'READ'
            """)
    int markAllAsRead(@Param("recipientUserId") Long recipientUserId, @Param("readAt") LocalDateTime readAt);
}
