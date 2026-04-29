package com.sism.iam.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.iam.domain.notification.UserNotification;
import com.sism.iam.domain.user.User;
import com.sism.iam.domain.notification.UserNotificationRepository;
import com.sism.iam.domain.user.UserRepository;
import com.sism.shared.domain.notification.NotificationProvider;
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
public class UserNotificationService implements NotificationProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TYPE_REMINDER = "REMINDER";
    private static final String TYPE_APPROVAL_APPROVED = "APPROVAL_APPROVED";
    private static final String TYPE_APPROVAL_REJECTED = "APPROVAL_REJECTED";
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
        String metadataJson = buildMetadataJson(
                indicatorId,
                indicatorName,
                targetOrgName,
                senderUserName,
                normalizedSource,
                reason
        );

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
    @Override
    public ApprovalResultNotification createApprovalResultNotification(
            Long recipientUserId,
            Long senderUserId,
            Long senderOrgId,
            Long approvalInstanceId,
            String entityType,
            Long entityId,
            String businessName,
            String stepName,
            boolean approved,
            String comment
    ) {
        if (recipientUserId == null) {
            throw new IllegalArgumentException("Recipient user ID is required");
        }

        String normalizedEntityType = entityType == null ? null : entityType.trim().toUpperCase();
        String resolvedBusinessName =
                businessName == null || businessName.isBlank()
                        ? (entityId == null ? "审批事项" : "业务对象#" + entityId)
                        : businessName.trim();
        String resolvedStepName =
                stepName == null || stepName.isBlank() ? "当前审批环节" : stepName.trim();
        String normalizedComment = comment == null ? "" : comment.trim();
        String actionUrl = approvalInstanceId == null
                ? "/messages"
                : "/messages?approvalInstanceId=" + approvalInstanceId;

        String title = resolveApprovalResultTitle(normalizedEntityType, approved);
        String content = approved
                ? "你已审批通过“" + resolvedBusinessName + "”，处理环节为“" + resolvedStepName + "”。"
                : "你已审批驳回“" + resolvedBusinessName + "”，处理环节为“" + resolvedStepName + "”。";
        if (!normalizedComment.isBlank()) {
            content = content + " 审批意见：" + normalizedComment;
        }

        UserNotification notification = new UserNotification();
        notification.setRecipientUserId(recipientUserId);
        notification.setSenderUserId(senderUserId);
        notification.setSenderOrgId(senderOrgId);
        notification.setNotificationType(approved ? TYPE_APPROVAL_APPROVED : TYPE_APPROVAL_REJECTED);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setStatus(STATUS_UNREAD);
        notification.setActionUrl(actionUrl);
        notification.setRelatedEntityType(normalizedEntityType);
        notification.setRelatedEntityId(entityId);
        notification.setMetadataJson(buildApprovalResultMetadataJson(
                approvalInstanceId,
                normalizedEntityType,
                entityId,
                resolvedBusinessName,
                resolvedStepName,
                approved,
                normalizedComment
        ));
        notification.validate();

        UserNotification saved = userNotificationRepository.save(notification);
        return new ApprovalResultNotification(
                saved.getId(),
                saved.getRecipientUserId(),
                approvalInstanceId,
                normalizedEntityType,
                entityId,
                saved.getNotificationType(),
                saved.getCreatedAt()
        );
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

    private static String buildMetadataJson(
            Long indicatorId,
            String indicatorName,
            String targetOrgName,
            String senderUserName,
            String source,
            String reason
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("indicatorId", indicatorId);
        metadata.put("indicatorName", indicatorName);
        metadata.put("targetOrgName", targetOrgName);
        metadata.put("senderUserName", senderUserName);
        metadata.put("source", source);
        metadata.put("reason", reason);
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize notification metadata", e);
        }
    }

    private String resolveApprovalResultTitle(String entityType, boolean approved) {
        String subject = switch (entityType == null ? "" : entityType) {
            case "PLAN" -> "计划审批";
            case "PLAN_REPORT" -> "月报审批";
            case "INDICATOR" -> "指标审批";
            case "INDICATOR_DISTRIBUTION" -> "指标下发审批";
            default -> "审批处理";
        };
        return subject + (approved ? "已通过" : "已驳回");
    }

    private static String buildApprovalResultMetadataJson(
            Long approvalInstanceId,
            String entityType,
            Long entityId,
            String businessName,
            String stepName,
            boolean approved,
            String comment
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("approvalInstanceId", approvalInstanceId);
        metadata.put("entityType", entityType);
        metadata.put("entityId", entityId);
        metadata.put("businessName", businessName);
        metadata.put("stepName", stepName);
        metadata.put("result", approved ? "APPROVED" : "REJECTED");
        metadata.put("comment", comment);
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize approval result metadata", e);
        }
    }
}
