package com.sism.vo;

import com.sism.enums.AdhocScopeType;
import com.sism.enums.AdhocTaskStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Value Object for adhoc task response
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
 */
@Data
public class AdhocTaskVO {

    private Long adhocTaskId;
    private Long cycleId;
    private String cycleName;
    private Long creatorOrgId;
    private String creatorOrgName;
    private AdhocScopeType scopeType;
    private Long indicatorId;
    private String indicatorDesc;
    private String taskTitle;
    private String taskDesc;
    private LocalDate openAt;
    private LocalDate dueAt;
    private Boolean includeInAlert;
    private Boolean requireIndicatorReport;
    private AdhocTaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Target organizations for this adhoc task
     */
    private List<AdhocTaskTargetVO> targets;

    /**
     * Mapped indicators for this adhoc task
     */
    private List<AdhocTaskIndicatorVO> indicators;

    /**
     * Nested VO for target organization
     */
    @Data
    public static class AdhocTaskTargetVO {
        private Long orgId;
        private String orgName;
    }

    /**
     * Nested VO for mapped indicator
     */
    @Data
    public static class AdhocTaskIndicatorVO {
        private Long indicatorId;
        private String indicatorDesc;
        private Long ownerOrgId;
        private String ownerOrgName;
    }
}
