package com.sism.iam.application.dto;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        String title,
        String content,
        String status,
        LocalDateTime scheduledAt,
        LocalDateTime publishedAt,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
