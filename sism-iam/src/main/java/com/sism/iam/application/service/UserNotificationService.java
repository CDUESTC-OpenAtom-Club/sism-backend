package com.sism.iam.application.service;

import com.sism.iam.domain.User;
import com.sism.iam.domain.UserNotification;
import com.sism.iam.domain.repository.UserNotificationRepository;
import com.sism.iam.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private static final String TYPE_REMINDER = "REMINDER";
    private static final String ENTITY_TYPE_INDICATOR = "INDICATOR";
    private static final String STATUS_READ = "READ";
    private static final String STATUS_UNREAD = "UNREAD";

    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;

    public record ReminderResult(
            Long reminderId,
            Long indicatorId,
            int sentCount,
            LocalDateTime lastRemindedAt,
            long remindCount,
            LocalDateTime cooldownUntil
    ) {}

    public record ReminderStatus(
            Long indicatorId,
            boolean canRemind,
            LocalDateTime lastRemindedAt,
            long remindCount,
            LocalDateTime cooldownUntil
    ) {}

    public Page<Map<String, Object>> getMyNotifications(Long userId, int page, int size, String status) {
        Pageable pageable = PaginationPolicy.toPageRequest(page, size);
        Page<UserNotification> notifications = status == null || status.isBlank()
                ? userNotificationRepository.findByRecipientUserId(userId, pageable)
                : userNotificationRepository.findByRecipientUserIdAndStatus(userId, status, pageable);

        return notifications.map(this::toNotificationPayload);
    }

    @Transactional
    public ReminderResult createReminderNotification(
            Long indicatorId,
            String indicatorName,
            Long targetOrgId,
            String targetOrgName,
            Long senderUserId,
            String senderUserName,
            Long senderOrgId,
            String source,
            String reason
    ) {
        if (indicatorId == null) {
            throw new IllegalArgumentException("Indicator ID is required");
        }
        Optional<UserNotification> latestRecord = userNotificationRepository.findLatestReminder(indicatorId, senderUserId);
        LocalDateTime now = LocalDateTime.now();
        if (latestRecord.isPresent() && latestRecord.get().getCreatedAt() != null
                && latestRecord.get().getCreatedAt().isAfter(now.minusHours(24))) {
            throw new IllegalStateException("该指标 24 小时内已催办，请稍后再试");
        }

        List<User> recipients = userRepository.findByOrgId(targetOrgId).stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .toList();
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("未找到可接收催办通知的目标组织用户");
        }

        String normalizedSource = source == null || source.isBlank() ? "DASHBOARD" : source;
        String batchKey = UUID.randomUUID().toString();
        String title = "滞后任务催办提醒";
        String content = "任务“" + indicatorName + "”当前进度滞后，请尽快处理并更新进展。";
        String metadataJson = """
                {"indicatorId":%d,"indicatorName":"%s","targetOrgName":"%s","senderUserName":"%s","source":"%s","reason":"%s"}
                """
                .formatted(
                        indicatorId,
                        escapeJson(indicatorName),
                        escapeJson(targetOrgName),
                        escapeJson(senderUserName),
                        escapeJson(normalizedSource),
                        escapeJson(reason)
                )
                .replace("\n", "")
                .trim();

        List<UserNotification> notifications = recipients.stream()
                .map(user -> buildReminderNotification(
                        user.getId(),
                        senderUserId,
                        senderOrgId,
                        indicatorId,
                        title,
                        content,
                        metadataJson,
                        batchKey
                ))
                .toList();
        List<UserNotification> savedNotifications = userNotificationRepository.saveAll(notifications);

        long remindCount = userNotificationRepository.countReminderBatches(indicatorId, senderUserId);
        LocalDateTime lastRemindedAt = savedNotifications.stream()
                .map(UserNotification::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(now);
        return new ReminderResult(
                savedNotifications.stream().map(UserNotification::getId).findFirst().orElse(null),
                indicatorId,
                notifications.size(),
                lastRemindedAt,
                remindCount,
                lastRemindedAt.plusHours(24)
        );
    }

    public Map<Long, ReminderStatus> getReminderStatuses(Collection<Long> indicatorIds, Long senderUserId) {
        if (indicatorIds == null || indicatorIds.isEmpty()) {
            return Map.of();
        }
        List<UserNotification> records = userNotificationRepository.findLatestReminders(indicatorIds, senderUserId);
        Map<Long, ReminderStatus> statuses = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (Long indicatorId : indicatorIds) {
            statuses.put(indicatorId, new ReminderStatus(indicatorId, true, null, 0, null));
        }

        for (UserNotification record : records) {
            long remindCount = userNotificationRepository.countReminderBatches(record.getRelatedEntityId(), senderUserId);
            LocalDateTime lastRemindedAt = record.getCreatedAt();
            LocalDateTime cooldownUntil = lastRemindedAt != null ? lastRemindedAt.plusHours(24) : null;
            statuses.put(
                    record.getRelatedEntityId(),
                    new ReminderStatus(
                            record.getRelatedEntityId(),
                            cooldownUntil == null || !cooldownUntil.isAfter(now),
                            lastRemindedAt,
                            remindCount,
                            cooldownUntil
                    )
            );
        }

        return statuses;
    }

    @Transactional
    public Map<String, Object> markNotificationAsRead(Long id, Long currentUserId) {
        UserNotification notification = userNotificationRepository.findByIdAndRecipientUserId(id, currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));
        LocalDateTime readAt = LocalDateTime.now();

        int updatedRows = userNotificationRepository.markAsRead(id, currentUserId, readAt);
        if (updatedRows == 0 && !STATUS_READ.equalsIgnoreCase(notification.getStatus())) {
            throw new IllegalArgumentException("Notification not found: " + id);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("notificationId", id);
        result.put("status", STATUS_READ);
        result.put("isRead", true);
        result.put("readAt", readAt);
        return result;
    }

    @Transactional
    public Map<String, Object> markAllNotificationsAsRead(Long currentUserId) {
        long readCount = userNotificationRepository.markAllAsRead(currentUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("readCount", readCount);
        result.put("timestamp", LocalDateTime.now());
        return result;
    }

    private UserNotification buildReminderNotification(
            Long recipientUserId,
            Long senderUserId,
            Long senderOrgId,
            Long indicatorId,
            String title,
            String content,
            String metadataJson,
            String batchKey
    ) {
        UserNotification notification = new UserNotification();
        notification.setRecipientUserId(recipientUserId);
        notification.setSenderUserId(senderUserId);
        notification.setSenderOrgId(senderOrgId);
        notification.setNotificationType(TYPE_REMINDER);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setStatus(STATUS_UNREAD);
        notification.setActionUrl("/indicators/" + indicatorId);
        notification.setRelatedEntityType(ENTITY_TYPE_INDICATOR);
        notification.setRelatedEntityId(indicatorId);
        notification.setBatchKey(batchKey);
        notification.setMetadataJson(metadataJson);
        notification.validate();
        return notification;
    }

    private Map<String, Object> toNotificationPayload(UserNotification notification) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", notification.getId());
        result.put("title", notification.getTitle());
        result.put("content", notification.getContent());
        result.put("status", notification.getStatus());
        result.put("isRead", STATUS_READ.equalsIgnoreCase(notification.getStatus()));
        result.put("createdAt", notification.getCreatedAt());
        result.put("type", notification.getNotificationType());
        result.put("link", notification.getActionUrl());
        result.put("relatedEntityType", notification.getRelatedEntityType());
        result.put("relatedEntityId", notification.getRelatedEntityId());
        return result;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
