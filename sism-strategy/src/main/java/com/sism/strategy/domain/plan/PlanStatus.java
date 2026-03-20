package com.sism.strategy.domain.plan;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Canonical business statuses for plans in the strategy context.
 */
public enum PlanStatus {
    DRAFT,
    PENDING,
    RETURNED,
    DISTRIBUTED;

    public static PlanStatus fromRaw(String rawStatus) {
        String normalized = normalizeRaw(rawStatus);
        return switch (normalized) {
            case "PENDING" -> PENDING;
            case "RETURNED" -> RETURNED;
            case "DISTRIBUTED" -> DISTRIBUTED;
            default -> DRAFT;
        };
    }

    public String value() {
        return name();
    }

    public static List<String> expandQueryStatuses(String rawStatus) {
        String normalized = normalizeRaw(rawStatus);
        Set<String> values = new LinkedHashSet<>();

        switch (normalized) {
            case "PENDING" -> {
                values.add(PENDING.value());
                values.add("IN_REVIEW");
                values.add("PENDING_APPROVAL");
                values.add("PENDING_REVIEW");
                values.add("SUBMITTED");
            }
            case "DISTRIBUTED" -> {
                values.add(DISTRIBUTED.value());
                values.add("APPROVED");
                values.add("ACTIVE");
                values.add("PUBLISHED");
            }
            case "RETURNED" -> {
                values.add(RETURNED.value());
                values.add("REJECTED");
                values.add("RETURNED");
            }
            default -> {
                values.add(DRAFT.value());
                values.add("WITHDRAWN");
                values.add("CANCELLED");
                values.add("COMPLETED");
                values.add("ARCHIVED");
            }
        }

        return List.copyOf(values);
    }

    private static String normalizeRaw(String rawStatus) {
        String value = rawStatus == null ? "" : rawStatus.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "PENDING", "IN_REVIEW", "PENDING_APPROVAL", "PENDING_REVIEW", "SUBMITTED" -> "PENDING";
            case "DISTRIBUTED", "APPROVED", "ACTIVE", "PUBLISHED" -> "DISTRIBUTED";
            case "RETURNED", "REJECTED" -> "RETURNED";
            case "DRAFT", "WITHDRAWN", "CANCELLED", "COMPLETED", "ARCHIVED" -> "DRAFT";
            default -> "DRAFT";
        };
    }
}
