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
        waitingStep.setStepNo(1);
        waitingStep.setStatus(AuditInstance.STEP_STATUS_WAITING);

        AuditStepInstance pendingStep = new AuditStepInstance();
        pendingStep.setStepNo(2);
        pendingStep.setStatus(AuditInstance.STEP_STATUS_PENDING);

        instance.addStepInstance(waitingStep);
        instance.addStepInstance(pendingStep);

        assertEquals(2, instance.resolveCurrentPendingStep().orElseThrow().getStepNo());
    }

    @Test
    @DisplayName("Should resolve latest pending step when multiple pending instances exist in history")
    void shouldResolveLatestPendingStepWhenMultiplePendingInstancesExistInHistory() {
        AuditInstance instance = new AuditInstance();

        AuditStepInstance earlierPendingStep = new AuditStepInstance();
        earlierPendingStep.setStepNo(2);
        earlierPendingStep.setStatus(AuditInstance.STEP_STATUS_PENDING);

        AuditStepInstance latestPendingStep = new AuditStepInstance();
        latestPendingStep.setStepNo(4);
        latestPendingStep.setStatus(AuditInstance.STEP_STATUS_PENDING);

        instance.addStepInstance(earlierPendingStep);
        instance.addStepInstance(latestPendingStep);

        assertEquals(4, instance.resolveCurrentPendingStep().orElseThrow().getStepNo());
    }

    @Test
    @DisplayName("Should allow requester withdraw after submit step auto-completed but before first approval handled")
    void shouldAllowRequesterWithdrawBeforeFirstApprovalHandled() {
        AuditInstance instance = new AuditInstance();
        instance.setStatus(AuditInstance.STATUS_PENDING);

        AuditStepInstance submitStep = new AuditStepInstance();
        submitStep.setStepNo(1);
        submitStep.setStatus(AuditInstance.STEP_STATUS_APPROVED);

        AuditStepInstance firstApproval = new AuditStepInstance();
        firstApproval.setStepNo(2);
        firstApproval.setStatus(AuditInstance.STEP_STATUS_PENDING);

        instance.addStepInstance(submitStep);
        instance.addStepInstance(firstApproval);

        assertTrue(instance.canRequesterWithdraw());
        assertDoesNotThrow(instance::cancel);
        assertEquals(AuditInstance.STATUS_WITHDRAWN, instance.getStatus());
        assertEquals(AuditInstance.STEP_STATUS_WITHDRAWN, submitStep.getStatus());
        assertEquals("提交人撤回", submitStep.getComment());
        assertNotNull(submitStep.getApprovedAt());
        assertEquals(AuditInstance.STEP_STATUS_WAITING, firstApproval.getStatus());
        assertNull(firstApproval.getComment());
        assertNull(firstApproval.getApprovedAt());
    }

    @Test
    @DisplayName("Should forbid requester withdraw after first approval handled")
    void shouldForbidRequesterWithdrawAfterFirstApprovalHandled() {
        AuditInstance instance = new AuditInstance();
        instance.setStatus(AuditInstance.STATUS_PENDING);

        AuditStepInstance submitStep = new AuditStepInstance();
        submitStep.setStepNo(1);
        submitStep.setStatus(AuditInstance.STEP_STATUS_APPROVED);

        AuditStepInstance firstApproval = new AuditStepInstance();
        firstApproval.setStepNo(2);
        firstApproval.setStatus(AuditInstance.STEP_STATUS_APPROVED);

        AuditStepInstance secondApproval = new AuditStepInstance();
        secondApproval.setStepNo(3);
        secondApproval.setStatus(AuditInstance.STEP_STATUS_PENDING);

        instance.addStepInstance(submitStep);
        instance.addStepInstance(firstApproval);
        instance.addStepInstance(secondApproval);

        assertFalse(instance.canRequesterWithdraw());
        assertThrows(IllegalStateException.class, instance::cancel);
    }

    @Test
    @DisplayName("Should keep rejected step history instead of reopening previous step in place")
    void shouldKeepRejectedStepHistoryInsteadOfReopeningPreviousStepInPlace() {
        AuditInstance instance = new AuditInstance();
        instance.setStatus(AuditInstance.STATUS_PENDING);

        AuditStepInstance submitStep = new AuditStepInstance();
        submitStep.setStepNo(1);
        submitStep.setStepDefId(11L);
        submitStep.setStatus(AuditInstance.STEP_STATUS_APPROVED);

        AuditStepInstance departmentStep = new AuditStepInstance();
        departmentStep.setStepNo(2);
        departmentStep.setStepDefId(12L);
        departmentStep.setStatus(AuditInstance.STEP_STATUS_APPROVED);

        AuditStepInstance leaderStep = new AuditStepInstance();
        leaderStep.setStepNo(3);
        leaderStep.setStepDefId(13L);
        leaderStep.setStatus(AuditInstance.STEP_STATUS_PENDING);

        instance.addStepInstance(submitStep);
        instance.addStepInstance(departmentStep);
        instance.addStepInstance(leaderStep);

        instance.reject(9L, "打回");

        assertEquals(AuditInstance.STEP_STATUS_APPROVED, departmentStep.getStatus());
        assertEquals(AuditInstance.STEP_STATUS_REJECTED, leaderStep.getStatus());
        assertEquals(3, instance.getStepInstances().size());
    }

    @Test
    @DisplayName("Should reactivate latest withdrawn step when submitter starts workflow again")
    void shouldReactivateLatestWithdrawnStep() {
        AuditInstance instance = new AuditInstance();
        instance.setStatus(AuditInstance.STATUS_WITHDRAWN);

        AuditStepInstance withdrawnStep = new AuditStepInstance();
        withdrawnStep.setStepNo(1);
        withdrawnStep.setStatus(AuditInstance.STEP_STATUS_WITHDRAWN);
        withdrawnStep.setComment("提交人撤回");
        withdrawnStep.setApprovedAt(java.time.LocalDateTime.now());

        AuditStepInstance waitingApproval = new AuditStepInstance();
        waitingApproval.setStepNo(2);
        waitingApproval.setStatus(AuditInstance.STEP_STATUS_WAITING);

        instance.addStepInstance(withdrawnStep);
        instance.addStepInstance(waitingApproval);

        instance.reactivateWithdrawnStep();

        assertEquals(AuditInstance.STATUS_PENDING, instance.getStatus());
        assertEquals(AuditInstance.STEP_STATUS_APPROVED, withdrawnStep.getStatus());
        assertEquals("系统自动完成提交流程节点", withdrawnStep.getComment());
        assertNotNull(withdrawnStep.getApprovedAt());
        assertEquals(AuditInstance.STEP_STATUS_PENDING, waitingApproval.getStatus());
        assertEquals(2, instance.resolveCurrentPendingStep().orElseThrow().getStepNo());
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
