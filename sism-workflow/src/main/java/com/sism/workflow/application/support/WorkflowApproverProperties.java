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
    private String planApprovePermissionCode;
    private String planReportApprovePermissionCode;
    private String indicatorDispatchApprovePermissionCode;
    private String indicatorReportApprovePermissionCode;
}
