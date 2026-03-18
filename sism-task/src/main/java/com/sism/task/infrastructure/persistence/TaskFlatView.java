package com.sism.task.infrastructure.persistence;

import java.time.LocalDateTime;

/**
 * Flat task projection used by read APIs that only need task table columns.
 */
public interface TaskFlatView {

    Long getId();

    String getTaskName();

    String getTaskDesc();

    String getTaskType();

    Long getPlanId();

    Long getCycleId();

    Long getOrgId();

    Long getCreatedByOrgId();

    Integer getSortOrder();

    String getStatus();

    String getRemark();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}
