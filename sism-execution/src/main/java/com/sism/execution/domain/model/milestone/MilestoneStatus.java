package com.sism.execution.domain.model.milestone;

import java.util.Arrays;

public enum MilestoneStatus {
    PLANNED,
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    DELAYED,
    CANCELLED;

    public static MilestoneStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(status -> status.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid milestone status: " + value));
    }

    public static String normalize(String value) {
        MilestoneStatus status = from(value);
        return status == null ? null : status.name();
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean isActive() {
        return this == PLANNED || this == NOT_STARTED || this == IN_PROGRESS || this == DELAYED;
    }
}
