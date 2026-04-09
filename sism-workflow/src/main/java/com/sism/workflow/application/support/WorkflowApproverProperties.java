package com.sism.workflow.application.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "workflow.approver")
public class WorkflowApproverProperties {

    private Long approverRoleId;
    private Long strategyDeptHeadRoleId;
    private Long vicePresidentRoleId;
    private Long strategyOrgId;
    private Map<Long, Long> functionalVicePresidentScopeByOrg = new LinkedHashMap<>();
    private String planApprovePermissionCode;
    private String planReportApprovePermissionCode;
    private String indicatorDispatchApprovePermissionCode;
    private String indicatorReportApprovePermissionCode;
}
