package com.sism.task.infrastructure.persistence;

import java.time.LocalDateTime;

/**
 * Flat task projection used by read APIs that only need task table columns.
 */
public interface TaskFlatView {

    Long getId();

    String getName();

    String getDesc();

    String getTaskType();

    Long getPlanId();

    Long getCycleId();

    Long getOrgId();

    Long getCreatedByOrgId();

    Integer getSortOrder();

    String getPlanStatus();

    String getTaskStatus();

    String getRemark();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}
