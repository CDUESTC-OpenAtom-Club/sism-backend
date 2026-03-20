package com.sism.workflow.domain;

import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditInstance Aggregate Root Tests")
class AuditInstanceTest {

    @Test
    @DisplayName("Should create AuditInstance with valid parameters")
    void shouldCreateAuditInstanceWithValidParameters() {
        AuditFlowDef flowDef = buildFlowDef();

        AuditInstance instance = AuditInstance.create(1L, "INDICATOR", flowDef);

        assertNotNull(instance);
        assertEquals(1L, instance.getEntityId());
        assertEquals("INDICATOR", instance.getEntityType());
        assertEquals(AuditInstance.STATUS_PENDING, instance.getStatus());
        assertNotNull(instance.getStartedAt());
    }

    @Test
    @DisplayName("Should throw exception when creating AuditInstance with invalid entity id")
    void shouldThrowExceptionWhenCreatingAuditInstanceWithInvalidEntityId() {
        AuditFlowDef flowDef = buildFlowDef();

        assertThrows(IllegalArgumentException.class, () ->
            AuditInstance.create(-1L, "INDICATOR", flowDef)
        );
    }

    @Test
    @DisplayName("Should validate AuditInstance with valid parameters")
    void shouldValidateAuditInstanceWithValidParameters() {
        AuditFlowDef flowDef = buildFlowDef();
        AuditInstance instance = AuditInstance.create(1L, "INDICATOR", flowDef);

        assertDoesNotThrow(instance::validate);
    }

    @Test
    @DisplayName("Should resolve current pending step from step instances instead of currentStepIndex")
    void shouldResolveCurrentPendingStepFromStepInstances() {
        AuditInstance instance = new AuditInstance();

        AuditStepInstance waitingStep = new AuditStepInstance();
        waitingStep.setStepIndex(1);
        waitingStep.setStatus(AuditInstance.STEP_STATUS_WAITING);

        AuditStepInstance pendingStep = new AuditStepInstance();
        pendingStep.setStepIndex(2);
        pendingStep.setStatus(AuditInstance.STEP_STATUS_PENDING);

        instance.addStepInstance(waitingStep);
        instance.addStepInstance(pendingStep);

        assertEquals(2, instance.resolveCurrentPendingStep().orElseThrow().getStepIndex());
    }

    @Test
    @DisplayName("Should allow requester withdraw after submit step auto-completed but before first approval handled")
    void shouldAllowRequesterWithdrawBeforeFirstApprovalHandled() {
        AuditInstance instance = new AuditInstance();
        instance.setStatus(AuditInstance.STATUS_PENDING);

        AuditStepInstance submitStep = new AuditStepInstance();
        submitStep.setStepIndex(1);
        submitStep.setStatus(AuditInstance.STEP_STATUS_APPROVED);

        AuditStepInstance firstApproval = new AuditStepInstance();
        firstApproval.setStepIndex(2);
        firstApproval.setStatus(AuditInstance.STEP_STATUS_PENDING);

        instance.addStepInstance(submitStep);
        instance.addStepInstance(firstApproval);

        assertTrue(instance.canRequesterWithdraw());
        assertDoesNotThrow(instance::cancel);
        assertEquals(AuditInstance.STATUS_WITHDRAWN, instance.getStatus());
    }

    @Test
    @DisplayName("Should forbid requester withdraw after first approval handled")
    void shouldForbidRequesterWithdrawAfterFirstApprovalHandled() {
        AuditInstance instance = new AuditInstance();
        instance.setStatus(AuditInstance.STATUS_PENDING);

        AuditStepInstance submitStep = new AuditStepInstance();
        submitStep.setStepIndex(1);
        submitStep.setStatus(AuditInstance.STEP_STATUS_APPROVED);

        AuditStepInstance firstApproval = new AuditStepInstance();
        firstApproval.setStepIndex(2);
        firstApproval.setStatus(AuditInstance.STEP_STATUS_APPROVED);

        AuditStepInstance secondApproval = new AuditStepInstance();
        secondApproval.setStepIndex(3);
        secondApproval.setStatus(AuditInstance.STEP_STATUS_PENDING);

        instance.addStepInstance(submitStep);
        instance.addStepInstance(firstApproval);
        instance.addStepInstance(secondApproval);

        assertFalse(instance.canRequesterWithdraw());
        assertThrows(IllegalStateException.class, instance::cancel);
    }

    private AuditFlowDef buildFlowDef() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setId(1L);
        flowDef.setFlowCode("FLOW_001");
        flowDef.setFlowName("测试流程");
        flowDef.setEntityType("INDICATOR");
        flowDef.setIsActive(true);
        return flowDef;
    }
}
