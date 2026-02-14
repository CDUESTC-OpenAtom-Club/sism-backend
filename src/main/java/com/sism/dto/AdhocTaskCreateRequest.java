package com.sism.dto;

import com.sism.enums.AdhocScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for creating an adhoc task
 * Requirements: 10.1, 10.2, 10.3, 10.4
 */
@Data
public class AdhocTaskCreateRequest {

    @NotNull(message = "Cycle ID is required")
    private Long cycleId;

    @NotNull(message = "Creator organization ID is required")
    private Long creatorOrgId;

    @NotNull(message = "Scope type is required")
    private AdhocScopeType scopeType = AdhocScopeType.ALL_ORGS;

    /**
     * Optional indicator ID for indicator-specific tasks
     */
    private Long indicatorId;

    @NotBlank(message = "Task title is required")
    @Size(max = 200, message = "Task title must not exceed 200 characters")
    private String taskTitle;

    private String taskDesc;

    private LocalDate openAt;

    private LocalDate dueAt;

    private Boolean includeInAlert = false;

    private Boolean requireIndicatorReport = false;

    /**
     * Target organization IDs for CUSTOM scope type
     * Used when scopeType is CUSTOM to specify target organizations
     */
    private List<Long> targetOrgIds;

    /**
     * Target indicator IDs for CUSTOM scope type
     * Used when scopeType is CUSTOM to specify target indicators
     */
    private List<Long> targetIndicatorIds;
}
