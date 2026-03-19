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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproverResolverTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void resolveApproverId_shouldReturnRequesterWhenRoleMissing() {
        AuditStepDef stepDef = new AuditStepDef();

        ApproverResolver resolver = new ApproverResolver(userRepository);

        assertEquals(1L, resolver.resolveApproverId(stepDef, 1L, 2L));
    }

    @Test
    void resolveApproverId_shouldPreferSameOrgRoleCandidate() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setRoleId(9L);

        User user = new User();
        user.setId(202L);
        user.setOrgId(30L);
        user.setIsActive(true);

        when(userRepository.findByRoleId(9L)).thenReturn(List.of(user));

        ApproverResolver resolver = new ApproverResolver(userRepository);

        assertEquals(202L, resolver.resolveApproverId(stepDef, 1L, 30L));
    }

    @Test
    void resolveApproverId_shouldFallbackToRequesterForLegacyNullType() {
        AuditStepDef stepDef = new AuditStepDef();

        ApproverResolver resolver = new ApproverResolver(userRepository);

        assertEquals(88L, resolver.resolveApproverId(stepDef, 88L, 30L));
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
