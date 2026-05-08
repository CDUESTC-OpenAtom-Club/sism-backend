package com.sism.shared.domain.notification;

import java.time.LocalDateTime;

/**
 * Shared notification capability contract for cross-context use cases.
 */
public interface NotificationProvider {

    record ApprovalResultNotification(
            Long notificationId,
            Long recipientUserId,
            Long approvalInstanceId,
            String entityType,
            Long entityId,
            String notificationType,
            LocalDateTime createdAt
    ) {}

    ApprovalResultNotification createApprovalResultNotification(
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
    );
}
