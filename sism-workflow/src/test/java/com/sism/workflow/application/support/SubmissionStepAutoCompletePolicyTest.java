package com.sism.workflow.application.support;

import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubmissionStepAutoCompletePolicyTest {

    @Test
    void apply_shouldAutoCompleteSubmissionStep() {
        AuditInstance instance = new AuditInstance();
        AuditStepDef firstStepDef = new AuditStepDef();
        firstStepDef.setStepType(AuditStepDef.STEP_TYPE_SUBMIT);

        AuditStepInstance submit = new AuditStepInstance();
        submit.setStepNo(1);
        submit.setStepName("填报人提交");
        submit.setStatus(AuditInstance.STEP_STATUS_PENDING);
        submit.setApproverId(11L);

        AuditStepInstance approve = new AuditStepInstance();
        approve.setStepNo(2);
        approve.setStepName("审核");
        approve.setStatus(AuditInstance.STEP_STATUS_WAITING);
        approve.setApproverId(12L);

        instance.addStepInstance(submit);
        instance.addStepInstance(approve);

        SubmissionStepAutoCompletePolicy policy = new SubmissionStepAutoCompletePolicy();
        policy.apply(instance, firstStepDef, 11L);

        assertEquals(AuditInstance.STEP_STATUS_APPROVED, submit.getStatus());
        assertEquals(AuditInstance.STEP_STATUS_PENDING, approve.getStatus());
        assertEquals(2, instance.resolveCurrentPendingStep().orElseThrow().getStepNo());
    }

    @Test
    void apply_shouldKeepPendingWhenNotSubmissionStep() {
        AuditInstance instance = new AuditInstance();
        AuditStepDef firstStepDef = new AuditStepDef();
        firstStepDef.setStepType(AuditStepDef.STEP_TYPE_APPROVAL);

        AuditStepInstance first = new AuditStepInstance();
        first.setStepNo(1);
        first.setStepName("一级审批");
        first.setStatus(AuditInstance.STEP_STATUS_PENDING);
        first.setApproverId(11L);
        instance.addStepInstance(first);

        SubmissionStepAutoCompletePolicy policy = new SubmissionStepAutoCompletePolicy();
        policy.apply(instance, firstStepDef, 11L);

        assertEquals(AuditInstance.STEP_STATUS_PENDING, first.getStatus());
        assertEquals(1, instance.resolveCurrentPendingStep().orElseThrow().getStepNo());
    }
}
