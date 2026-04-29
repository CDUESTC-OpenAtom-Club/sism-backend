package com.sism.task.domain.task;

import java.util.Arrays;

/**
 * Strategic task lifecycle status.
 */
public enum TaskStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
    CANCELLED;

    public String value() {
        return name();
    }

    public static TaskStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(status -> status.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid task status: " + value));
    }

    public static String normalize(String value) {
        TaskStatus status = from(value);
        return status == null ? null : status.value();
    }
}
