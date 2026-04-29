package com.sism.execution.domain.report;

public record PlanReportIndicatorSnapshot(
        Long indicatorId,
        Integer progress,
        String comment,
        String milestoneNote,
        java.util.List<PlanReportAttachmentSnapshot> attachments
) {
}
