package com.sism.shared.domain.model.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditInstanceTest {

    @Test
    void approveShouldAdvanceToNextStepBeforeFinishingWorkflow() {
        AuditInstance instance = new AuditInstance();
        instance.setEntityType("PLAN");
        instance.setEntityId(1L);
        instance.setCurrentStepIndex(0);

        AuditStepInstance first = new AuditStepInstance();
        first.setStepIndex(0);
        first.setStatus(AuditInstance.STATUS_PENDING);
        instance.addStepInstance(first);

        AuditStepInstance second = new AuditStepInstance();
        second.setStepIndex(1);
        second.setStatus(AuditInstance.STATUS_PENDING);
        instance.addStepInstance(second);

        instance.approve(100L, "ok");

        assertEquals(AuditInstance.STATUS_PENDING, instance.getStatus());
        assertEquals(1, instance.getCurrentStepIndex());
        assertEquals(AuditInstance.STATUS_APPROVED, first.getStatus());
        assertNull(instance.getCompletedAt());
    }

    @Test
    void approveShouldFinishWorkflowOnLastStep() {
        AuditInstance instance = new AuditInstance();
        instance.setEntityType("PLAN");
        instance.setEntityId(1L);
        instance.setCurrentStepIndex(0);

        AuditStepInstance onlyStep = new AuditStepInstance();
        onlyStep.setStepIndex(0);
        onlyStep.setStatus(AuditInstance.STATUS_PENDING);
        instance.addStepInstance(onlyStep);

        instance.approve(100L, "done");

        assertEquals(AuditInstance.STATUS_APPROVED, instance.getStatus());
        assertNotNull(instance.getCompletedAt());
        assertEquals(AuditInstance.STATUS_APPROVED, onlyStep.getStatus());
    }

    @Test
    void transferAndAddApproverShouldFailFast() {
        AuditInstance instance = new AuditInstance();

        assertThrows(UnsupportedOperationException.class, () -> instance.transfer(1L));
        assertThrows(UnsupportedOperationException.class, () -> instance.addApprover(1L));
    }
}
