package com.sism.dto;

import com.sism.enums.AdhocScopeType;
import com.sism.enums.AdhocTaskStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for updating an adhoc task
 * Requirements: 10.4, 10.5
 */
@Data
public class AdhocTaskUpdateRequest {

    private AdhocScopeType scopeType;

    private Long indicatorId;

    @Size(max = 200, message = "Task title must not exceed 200 characters")
    private String taskTitle;

    private String taskDesc;

    private LocalDate openAt;

    private LocalDate dueAt;

    private Boolean includeInAlert;

    private Boolean requireIndicatorReport;

    private AdhocTaskStatus status;

    /**
     * Target organization IDs for CUSTOM scope type
     * Used when scopeType is CUSTOM to update target organizations
     */
    private List<Long> targetOrgIds;

    /**
     * Target indicator IDs for CUSTOM scope type
     * Used when scopeType is CUSTOM to update target indicators
     */
    private List<Long> targetIndicatorIds;
}
