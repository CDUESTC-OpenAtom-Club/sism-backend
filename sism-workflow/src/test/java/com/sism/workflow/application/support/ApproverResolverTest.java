package com.sism.workflow.application.support;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.strategy.domain.plan.Plan;
import com.sism.strategy.domain.repository.PlanRepository;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
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
    private UserRepository userRepository;

    @Mock
    private PlanRepository planRepository;

    @Test
    void resolveApproverId_shouldRejectWhenRoleMissing() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setStepName("战略发展部负责人审批");

        ApproverResolver resolver = new ApproverResolver(userRepository, planRepository);

        assertThrows(IllegalStateException.class, () -> resolver.resolveApproverId(stepDef, 1L, 2L));
    }

    @Test
    void resolveApproverId_shouldPreferSameOrgRoleCandidate() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(2L);
        stepDef.setStepName("职能部门审批人审批");

        User user = new User();
        user.setId(202L);
        user.setOrgId(30L);
        user.setIsActive(true);

        when(userRepository.findByRoleId(2L)).thenReturn(List.of(user));

        ApproverResolver resolver = new ApproverResolver(userRepository, planRepository);

        assertEquals(202L, resolver.resolveApproverId(stepDef, 1L, 30L));
    }

    @Test
    void resolveApproverId_shouldPreferSameOrgCollegeLeaderByRoleScope() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(4L);
        stepDef.setStepName("学院院长审批人审批");

        User otherCollegeLeader = new User();
        otherCollegeLeader.setId(372L);
        otherCollegeLeader.setOrgId(57L);
        otherCollegeLeader.setIsActive(true);

        User sameCollegeLeader = new User();
        sameCollegeLeader.setId(369L);
        sameCollegeLeader.setOrgId(56L);
        sameCollegeLeader.setIsActive(true);

        when(userRepository.findByRoleId(4L)).thenReturn(List.of(otherCollegeLeader, sameCollegeLeader));

        ApproverResolver resolver = new ApproverResolver(userRepository, planRepository);

        assertEquals(369L, resolver.resolveApproverId(stepDef, 188L, 56L));
    }

    @Test
    void resolveApproverId_shouldRejectWhenRoleHasNoCandidates() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(4L);
        stepDef.setStepName("分管校领导审批");

        ApproverResolver resolver = new ApproverResolver(userRepository, planRepository);

        when(userRepository.findByRoleId(4L)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> resolver.resolveApproverId(stepDef, 88L, 30L));
    }

    @Test
    void resolveApproverId_shouldResolveVicePresidentByDedicatedRole() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(4L);
        stepDef.setStepName("分管校领导审批");

        User sameOrgLeader = new User();
        sameOrgLeader.setId(300L);
        sameOrgLeader.setOrgId(44L);
        sameOrgLeader.setIsActive(true);

        User strategyLeader = new User();
        strategyLeader.setId(124L);
        strategyLeader.setOrgId(35L);
        strategyLeader.setIsActive(true);

        when(userRepository.findByRoleId(4L)).thenReturn(List.of(sameOrgLeader, strategyLeader));

        ApproverResolver resolver = new ApproverResolver(userRepository, planRepository);

        assertEquals(124L, resolver.resolveApproverId(stepDef, 223L, 44L));
    }

    @Test
    void resolveApproverName_shouldReturnRealNameWhenAvailable() {
        User user = new User();
        user.setId(300L);
        user.setRealName("审批人");
        when(userRepository.findById(300L)).thenReturn(Optional.of(user));

        ApproverResolver resolver = new ApproverResolver(userRepository, planRepository);

        assertEquals("审批人", resolver.resolveApproverName(300L));
    }

    @Test
    void resolveApproverId_shouldUsePlanCreatorOrgForCollegeFinalApprovalStep() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(2L);
        stepDef.setStepName("职能部门终审人审批");

        AuditInstance instance = new AuditInstance();
        instance.setEntityType("PLAN");
        instance.setEntityId(7057L);

        Plan plan = new Plan();
        plan.setId(7057L);
        plan.setCreatedByOrgId(44L);

        User collegeApprover = new User();
        collegeApprover.setId(370L);
        collegeApprover.setOrgId(57L);
        collegeApprover.setIsActive(true);

        User functionalApprover = new User();
        functionalApprover.setId(267L);
        functionalApprover.setOrgId(44L);
        functionalApprover.setIsActive(true);

        when(planRepository.findById(7057L)).thenReturn(Optional.of(plan));
        when(userRepository.findByRoleId(2L)).thenReturn(List.of(collegeApprover, functionalApprover));

        ApproverResolver resolver = new ApproverResolver(userRepository, planRepository);

        assertEquals(267L, resolver.resolveApproverId(stepDef, 188L, 57L, instance));
    }
}
