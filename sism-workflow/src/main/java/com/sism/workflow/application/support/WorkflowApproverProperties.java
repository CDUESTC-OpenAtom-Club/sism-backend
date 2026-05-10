package com.sism.workflow.application.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Validated
@ConfigurationProperties(prefix = "workflow.approver")
public class WorkflowApproverProperties {

    @NotNull
    private Long approverRoleId;
    @NotNull
    private Long strategyDeptHeadRoleId;
    @NotNull
    private Long vicePresidentRoleId;
    @NotNull
    private Long strategyOrgId;
    private Map<Long, Long> functionalVicePresidentScopeByOrg = new LinkedHashMap<>();
    @Deprecated(since = "2026-05", forRemoval = false)
    private String planApprovePermissionCode;
    @Deprecated(since = "2026-05", forRemoval = false)
    private String planReportApprovePermissionCode;
    @Deprecated(since = "2026-05", forRemoval = false)
    private String indicatorDispatchApprovePermissionCode;
    @Deprecated(since = "2026-05", forRemoval = false)
    private String indicatorReportApprovePermissionCode;
}
