package com.sism.iam.application.dto;

import java.time.LocalDateTime;

public record UpdateAnnouncementRequest(
        String title,
        String content,
        LocalDateTime scheduledAt
) {}
