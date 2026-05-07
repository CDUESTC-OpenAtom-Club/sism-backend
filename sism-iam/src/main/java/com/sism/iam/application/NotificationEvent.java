package com.sism.iam.application;

/**
 * Lightweight notification event reserved for future notification-center integration.
 */
public record NotificationEvent(
        String title,
        String content,
        String userEmail,
        boolean emailEnabled
) {
}
