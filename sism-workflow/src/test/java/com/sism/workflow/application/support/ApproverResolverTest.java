package com.sism.workflow.application.support;

import com.sism.shared.domain.user.UserIdentity;
import com.sism.shared.domain.user.UserProvider;
import com.sism.shared.domain.workflow.WorkflowBusinessContextPort;
import com.sism.workflow.domain.definition.AuditStepDef;
import com.sism.workflow.domain.runtime.AuditInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproverResolverTest {

    @Mock
    private UserProvider userProvider;

    @Mock
    private WorkflowBusinessContextPort workflowBusinessContextPort;

    @Test
    void resolveApproverId_shouldRejectWhenRoleMissing() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setStepName("战略发展部负责人审批");

        ApproverResolver resolver = new ApproverResolver(
                userProvider,
                List.of(workflowBusinessContextPort),
                workflowApproverProperties()
        );

        assertThrows(IllegalStateException.class, () -> resolver.resolveApproverId(stepDef, 1L, 2L));
    }

    @Test
    void resolveApproverId_shouldPreferSameOrgRoleCandidate() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(2L);
        stepDef.setStepName("职能部门审批人审批");

        UserIdentity user = new UserIdentity(202L, "user202", "审批人202", 30L, true);

        when(userProvider.findActiveIdentitiesByRole(2L)).thenReturn(List.of(user));

        ApproverResolver resolver = new ApproverResolver(
                userProvider,
                List.of(workflowBusinessContextPort),
                workflowApproverProperties()
        );

        assertEquals(202L, resolver.resolveApproverId(stepDef, 1L, 30L));
    }

    @Test
    void resolveApproverId_shouldPreferSameOrgCollegeLeaderByRoleScope() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(4L);
        stepDef.setStepName("学院院长审批人审批");

        UserIdentity otherCollegeLeader = new UserIdentity(372L, "u372", "Leader372", 57L, true);
        UserIdentity sameCollegeLeader = new UserIdentity(369L, "u369", "Leader369", 56L, true);

        when(userProvider.findActiveIdentitiesByRole(4L)).thenReturn(List.of(otherCollegeLeader, sameCollegeLeader));

        ApproverResolver resolver = new ApproverResolver(
                userProvider,
                List.of(workflowBusinessContextPort),
                workflowApproverProperties()
        );

        assertEquals(369L, resolver.resolveApproverId(stepDef, 188L, 56L));
    }

    @Test
    void resolveApproverId_shouldRejectWhenRoleHasNoCandidates() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(4L);
        stepDef.setStepName("分管校领导审批");

        ApproverResolver resolver = new ApproverResolver(
                userProvider,
                List.of(workflowBusinessContextPort),
                workflowApproverProperties()
        );

        when(userProvider.findActiveIdentitiesByRole(4L)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> resolver.resolveApproverId(stepDef, 88L, 30L));
    }

    @Test
    void resolveApproverId_shouldResolveFunctionalVicePresidentByRequesterOrgMapping() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(4L);
        stepDef.setStepName("分管校领导审批");

        UserIdentity sameOrgLeader = new UserIdentity(300L, "u300", "Leader300", 44L, true);
        UserIdentity strategyLeader = new UserIdentity(124L, "u124", "Leader124", 35L, true);

        when(userProvider.findActiveIdentitiesByRole(4L)).thenReturn(List.of(sameOrgLeader, strategyLeader));

        ApproverResolver resolver = new ApproverResolver(
                userProvider,
                List.of(workflowBusinessContextPort),
                workflowApproverProperties()
        );

        assertEquals(300L, resolver.resolveApproverId(stepDef, 223L, 44L));
    }

    @Test
    void resolveApproverId_shouldKeepStrategyVicePresidentOnStrategyOrg() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(4L);
        stepDef.setStepName("分管校领导审批");

        UserIdentity strategyLeader = new UserIdentity(124L, "u124", "Leader124", 35L, true);
        UserIdentity functionalLeader = new UserIdentity(326L, "u326", "Leader326", 44L, true);

        when(userProvider.findActiveIdentitiesByRole(4L)).thenReturn(List.of(functionalLeader, strategyLeader));

        ApproverResolver resolver = new ApproverResolver(
                userProvider,
                List.of(workflowBusinessContextPort),
                workflowApproverProperties()
        );

        assertEquals(124L, resolver.resolveApproverId(stepDef, 188L, 35L));
    }

    @Test
    void resolveApproverName_shouldReturnRealNameWhenAvailable() {
        UserIdentity user = new UserIdentity(300L, "u300", "审批人", 35L, true);
        when(userProvider.findIdentity(300L)).thenReturn(Optional.of(user));

        ApproverResolver resolver = new ApproverResolver(
                userProvider,
                List.of(workflowBusinessContextPort),
                workflowApproverProperties()
        );

        assertEquals("审批人", resolver.resolveApproverName(300L));
    }

    @Test
    void resolveApproverId_shouldUsePlanCreatorOrgForCollegeFinalApprovalStep() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(2L);
        stepDef.setStepName("职能部门终审人审批");
        stepDef.setIsTerminal(true);

        AuditInstance instance = new AuditInstance();
        instance.setEntityType("PLAN");
        instance.setEntityId(7057L);

        UserIdentity collegeApprover = new UserIdentity(370L, "u370", "College370", 57L, true);
        UserIdentity functionalApprover = new UserIdentity(267L, "u267", "Func267", 44L, true);

        when(workflowBusinessContextPort.getBusinessSummary("PLAN", 7057L))
                .thenReturn(Optional.of(new WorkflowBusinessContextPort.BusinessSummary(7057L, "Plan 7057", 44L, "教务处", 57L, "学院", "Plan 7057")));
        when(userProvider.findActiveIdentitiesByRole(2L)).thenReturn(List.of(collegeApprover, functionalApprover));

        ApproverResolver resolver = new ApproverResolver(
                userProvider,
                List.of(workflowBusinessContextPort),
                workflowApproverProperties()
        );

        assertEquals(267L, resolver.resolveApproverId(stepDef, 188L, 57L, instance));
    }

    @Test
    void resolveApproverId_shouldUseTerminalMetadataBeforeLegacyStepNameForCollegeFinalApproval() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(2L);
        stepDef.setStepName("任意终审节点");
        stepDef.setIsTerminal(true);

        AuditInstance instance = new AuditInstance();
        instance.setEntityType("PLAN");
        instance.setEntityId(8088L);

        UserIdentity creatorOrgApprover = new UserIdentity(267L, "u267", "Func267", 44L, true);
        UserIdentity requesterOrgApprover = new UserIdentity(370L, "u370", "Req370", 57L, true);

        when(workflowBusinessContextPort.getBusinessSummary("PLAN", 8088L))
                .thenReturn(Optional.of(new WorkflowBusinessContextPort.BusinessSummary(8088L, "Plan 8088", 44L, "教务处", 57L, "学院", "Plan 8088")));
        when(userProvider.findActiveIdentitiesByRole(2L)).thenReturn(List.of(requesterOrgApprover, creatorOrgApprover));

        ApproverResolver resolver = new ApproverResolver(
                userProvider,
                List.of(workflowBusinessContextPort),
                workflowApproverProperties()
        );

        assertEquals(267L, resolver.resolveApproverId(stepDef, 188L, 57L, instance));
    }
    private WorkflowApproverProperties workflowApproverProperties() {
        WorkflowApproverProperties properties = new WorkflowApproverProperties();
        properties.setApproverRoleId(2L);
        properties.setStrategyDeptHeadRoleId(3L);
        properties.setVicePresidentRoleId(4L);
        properties.setStrategyOrgId(35L);
        properties.setFunctionalVicePresidentScopeByOrg(java.util.Map.of());
        return properties;
    }
}
