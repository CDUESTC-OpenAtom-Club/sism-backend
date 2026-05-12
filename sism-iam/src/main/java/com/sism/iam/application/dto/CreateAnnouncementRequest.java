package com.sism.iam.application.dto;

import java.time.LocalDateTime;

public record CreateAnnouncementRequest(
        String title,
        String content,
        LocalDateTime scheduledAt
) {}
