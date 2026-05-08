package com.sism.execution.domain.report;

public record PlanReportAttachmentSnapshot(
        Long id,
        String fileName,
        Long fileSize,
        String fileType,
        String url,
        Long uploadedBy,
        String uploadedAt
) {
}
