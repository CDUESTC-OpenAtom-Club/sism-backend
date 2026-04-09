package com.sism.iam.application.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * Test-scope shim for the strategy module.
 * The current workspace only needs the type to exist while strategy tests run.
 */
public class UserNotificationService {

    public record ReminderResult(
            Long reminderId,
            Long indicatorId,
            int sentCount,
            LocalDateTime lastRemindedAt,
            int remindCount,
            LocalDateTime cooldownUntil
    ) {}

    public record ReminderStatus(
            boolean canRemind,
            LocalDateTime lastRemindedAt,
            int remindCount,
            LocalDateTime cooldownUntil
    ) {}

    public ReminderResult createReminderNotification(
            Long indicatorId,
            String indicatorDesc,
            Long targetOrgId,
            String targetOrgName,
            Long currentUserId,
            String currentUserName,
            Long currentUserOrgId,
            String source,
            String reason
    ) {
        throw new UnsupportedOperationException("Test shim only");
    }

    public Map<Long, ReminderStatus> getReminderStatuses(Collection<Long> indicatorIds, Long currentUserId) {
        throw new UnsupportedOperationException("Test shim only");
    }
}
