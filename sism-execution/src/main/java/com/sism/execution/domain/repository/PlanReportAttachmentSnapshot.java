package com.sism.execution.domain.repository;

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
