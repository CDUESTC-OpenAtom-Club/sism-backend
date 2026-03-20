package com.sism.workflow.application.support;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.workflow.domain.definition.model.AuditStepDef;
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

    @Test
    void resolveApproverId_shouldRejectWhenRoleMissing() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setStepName("战略发展部负责人审批");

        ApproverResolver resolver = new ApproverResolver(userRepository);

        assertThrows(IllegalStateException.class, () -> resolver.resolveApproverId(stepDef, 1L, 2L));
    }

    @Test
    void resolveApproverId_shouldPreferSameOrgRoleCandidate() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(6L);
        stepDef.setStepName("职能部门审批人审批");

        User user = new User();
        user.setId(202L);
        user.setOrgId(30L);
        user.setIsActive(true);

        when(userRepository.findByRoleId(6L)).thenReturn(List.of(user));

        ApproverResolver resolver = new ApproverResolver(userRepository);

        assertEquals(202L, resolver.resolveApproverId(stepDef, 1L, 30L));
    }

    @Test
    void resolveApproverId_shouldRejectWhenRoleHasNoCandidates() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(7L);
        stepDef.setStepName("分管校领导审批");

        ApproverResolver resolver = new ApproverResolver(userRepository);

        when(userRepository.findByRoleId(7L)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> resolver.resolveApproverId(stepDef, 88L, 30L));
    }

    @Test
    void resolveApproverId_shouldPreferStrategyOrgForVicePresidentStep() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(7L);
        stepDef.setStepName("分管校领导审批");

        User sameOrgLeader = new User();
        sameOrgLeader.setId(300L);
        sameOrgLeader.setOrgId(44L);
        sameOrgLeader.setIsActive(true);

        User strategyLeader = new User();
        strategyLeader.setId(124L);
        strategyLeader.setOrgId(35L);
        strategyLeader.setIsActive(true);

        when(userRepository.findByRoleId(7L)).thenReturn(List.of(sameOrgLeader, strategyLeader));

        ApproverResolver resolver = new ApproverResolver(userRepository);

        assertEquals(124L, resolver.resolveApproverId(stepDef, 223L, 44L));
    }

    @Test
    void resolveApproverName_shouldReturnRealNameWhenAvailable() {
        User user = new User();
        user.setId(300L);
        user.setRealName("审批人");
        when(userRepository.findById(300L)).thenReturn(Optional.of(user));

        ApproverResolver resolver = new ApproverResolver(userRepository);

        assertEquals("审批人", resolver.resolveApproverName(300L));
    }
}
