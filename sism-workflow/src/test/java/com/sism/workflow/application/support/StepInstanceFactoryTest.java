package com.sism.workflow.application.support;

import com.sism.iam.domain.repository.UserRepository;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class StepInstanceFactoryTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void initialize_shouldInferSubmitStepFromLegacyName() {
        AuditFlowDef flowDef = new AuditFlowDef();
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setId(1L);
        stepDef.setStepName("填报人提交");
        stepDef.setStepOrder(1);
        flowDef.setSteps(List.of(stepDef));

        StepInstanceFactory factory = new StepInstanceFactory(
                new ApproverResolver(userRepository),
                new SubmissionStepAutoCompletePolicy()
        );

        AuditInstance instance = new AuditInstance();
        instance.setEntityType("PlanReport");
        instance.setEntityId(1L);

        assertDoesNotThrow(() -> factory.initialize(instance, flowDef, 1L, 1L));
        assertEquals(1, instance.getStepInstances().size());
        assertEquals(AuditInstance.STEP_STATUS_APPROVED, instance.getStepInstances().get(0).getStatus());
    }

    @Test
    void initialize_shouldFallbackApprovalStepToRequesterWhenRoleIdMissing() {
        AuditFlowDef flowDef = new AuditFlowDef();
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setId(1L);
        stepDef.setStepName("审批");
        stepDef.setStepOrder(1);
        stepDef.setStepType(AuditStepDef.STEP_TYPE_APPROVAL);
        flowDef.setSteps(List.of(stepDef));

        StepInstanceFactory factory = new StepInstanceFactory(
                new ApproverResolver(userRepository),
                new SubmissionStepAutoCompletePolicy()
        );

        AuditInstance instance = new AuditInstance();
        instance.setEntityType("PlanReport");
        instance.setEntityId(1L);

        assertDoesNotThrow(() -> factory.initialize(instance, flowDef, 1L, 1L));
        assertEquals(1L, instance.getStepInstances().get(0).getApproverId());
    }

    @Test
    void initialize_shouldAllowSubmitStepWithoutRoleId() {
        AuditFlowDef flowDef = new AuditFlowDef();
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setId(1L);
        stepDef.setStepName("提交");
        stepDef.setStepOrder(1);
        stepDef.setStepType(AuditStepDef.STEP_TYPE_SUBMIT);
        flowDef.setSteps(List.of(stepDef));

        StepInstanceFactory factory = new StepInstanceFactory(
                new ApproverResolver(userRepository),
                new SubmissionStepAutoCompletePolicy()
        );

        AuditInstance instance = new AuditInstance();
        instance.setEntityType("PlanReport");
        instance.setEntityId(1L);

        assertDoesNotThrow(() -> factory.initialize(instance, flowDef, 1L, 1L));
    }
}
