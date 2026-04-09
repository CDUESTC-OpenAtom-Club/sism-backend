package com.sism.alert.domain.enums;

import java.util.Locale;

/**
 * Alert status enumeration.
 * Keeps legacy aliases compatible while exposing canonical enum values.
 */
public enum AlertStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    /**
     * Normalize legacy or canonical status text to an enum value.
     *
     * @param status status text from API or persistence
     * @return matching status enum, or {@code null} if unsupported
     */
    public static AlertStatus from(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "PENDING", "OPEN" -> OPEN;
            case "TRIGGERED", "IN_PROGRESS" -> IN_PROGRESS;
            case "RESOLVED" -> RESOLVED;
            case "CLOSED" -> CLOSED;
            default -> null;
        };
    }
}
