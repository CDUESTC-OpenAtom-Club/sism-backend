package com.sism.execution.domain.model.plan;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Plan business status.
 *
 * <p>Plan 只对外暴露三种业务状态：
 * DRAFT（草稿）、PENDING（待审批）、DISTRIBUTED（已下发）。
 * 其它流程状态属于审批域，需要在审批表中维护；这里保留遗留别名的兼容映射，
 * 以便平滑读取历史数据和兼容旧接口入参。</p>
 */
public enum PlanStatus {
    DRAFT,
    PENDING,
    DISTRIBUTED;

    public static PlanStatus fromRaw(String rawStatus) {
        String normalized = normalizeRaw(rawStatus);
        return switch (normalized) {
            case "PENDING" -> PENDING;
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
            default -> {
                values.add(DRAFT.value());
                values.add("REJECTED");
                values.add("RETURNED");
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
            case "DRAFT", "REJECTED", "RETURNED", "WITHDRAWN", "CANCELLED", "COMPLETED", "ARCHIVED" -> "DRAFT";
            default -> "DRAFT";
        };
    }
}
