package com.sism.analytics.application;

import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Objects;

abstract class BaseApplicationService {

    protected void requirePositiveUserId(Long userId, String fieldName) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive number");
        }
    }

    protected void requireUserOwnership(Long requestedUserId, Long currentUserId, String errorMessage) {
        requirePositiveUserId(requestedUserId, "Requested user ID");
        requirePositiveUserId(currentUserId, "Current user ID");
        if (!Objects.equals(requestedUserId, currentUserId)) {
            throw new AccessDeniedException(errorMessage != null ? errorMessage : "No permission to access this resource");
        }
    }

    protected void requireUserOwnership(Long requestedUserId, Long currentUserId) {
        requireUserOwnership(requestedUserId, currentUserId, null);
    }

    protected void requireValidDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }
}
