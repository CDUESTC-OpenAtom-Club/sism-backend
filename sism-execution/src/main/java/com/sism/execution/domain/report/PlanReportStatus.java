package com.sism.execution.domain.report;

import java.util.Arrays;

/**
 * 计划报告状态。
 */
public enum PlanReportStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED;

    public static PlanReportStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase();
        if ("IN_REVIEW".equals(normalized)) {
            return SUBMITTED;
        }

        return Arrays.stream(values())
                .filter(status -> status.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan report status: " + value));
    }

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED;
    }
}
