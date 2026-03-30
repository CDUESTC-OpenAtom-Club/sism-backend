package com.sism.execution.domain.repository;

public record PlanReportIndicatorSnapshot(
        Long indicatorId,
        Integer progress,
        String comment,
        String milestoneNote,
        java.util.List<PlanReportAttachmentSnapshot> attachments
) {
}
