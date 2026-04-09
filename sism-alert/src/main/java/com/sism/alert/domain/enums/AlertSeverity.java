package com.sism.alert.domain.enums;

import java.util.Locale;

/**
 * Alert severity enumeration
 * Defines the severity levels of alert events
 */
public enum AlertSeverity {
    /**
     * Informational alert - gap <= 10%
     */
    INFO,

    /**
     * Warning alert - gap 10-20%
     */
    WARNING,

    /**
     * Critical alert - gap > 20%
     */
    CRITICAL;

    /**
     * Normalizes legacy alert severity labels to the canonical database vocabulary.
     *
     * @param severity severity label from API or persistence
     * @return canonical severity label or {@code null} when unsupported
     */
    public static String normalize(String severity) {
        if (severity == null || severity.trim().isEmpty()) {
            return null;
        }

        return switch (severity.trim().toUpperCase(Locale.ROOT)) {
            case "MAJOR", "WARNING" -> WARNING.name();
            case "MINOR", "INFO" -> INFO.name();
            case "CRITICAL" -> CRITICAL.name();
            default -> null;
        };
    }
}
