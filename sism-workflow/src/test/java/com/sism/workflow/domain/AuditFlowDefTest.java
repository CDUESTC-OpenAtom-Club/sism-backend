package com.sism.workflow.domain;

import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditFlowDefTest {

    @Test
    void validate_shouldPassForSubmitThenApprovalSteps() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setFlowCode("PLAN_DISPATCH");
        flowDef.setFlowName("计划下发");
        flowDef.setEntityType("PLAN");

        flowDef.addStep(step(1, "提交", AuditStepDef.STEP_TYPE_SUBMIT, null));
        flowDef.addStep(step(2, "一级审批", AuditStepDef.STEP_TYPE_APPROVAL, 8L));
        flowDef.addStep(step(3, "二级审批", AuditStepDef.STEP_TYPE_APPROVAL, 9L));

        assertDoesNotThrow(flowDef::validate);
    }

    @Test
    void validate_shouldFailWhenFirstStepIsNotSubmit() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setFlowCode("PLAN_DISPATCH");
        flowDef.setFlowName("计划下发");
        flowDef.setEntityType("PLAN");
        flowDef.addStep(step(1, "一级审批", AuditStepDef.STEP_TYPE_APPROVAL, 8L));

        assertThrows(IllegalArgumentException.class, flowDef::validate);
    }

    @Test
    void validate_shouldFailWhenApprovalStepMissingRole() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setFlowCode("PLAN_DISPATCH");
        flowDef.setFlowName("计划下发");
        flowDef.setEntityType("PLAN");
        flowDef.addStep(step(1, "提交", AuditStepDef.STEP_TYPE_SUBMIT, null));
        flowDef.addStep(step(2, "一级审批", AuditStepDef.STEP_TYPE_APPROVAL, null));

        assertThrows(IllegalArgumentException.class, flowDef::validate);
    }

    @Test
    void validate_shouldFailWhenStepOrderIsNotContinuous() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setFlowCode("PLAN_DISPATCH");
        flowDef.setFlowName("计划下发");
        flowDef.setEntityType("PLAN");
        flowDef.addStep(step(1, "提交", AuditStepDef.STEP_TYPE_SUBMIT, null));
        flowDef.addStep(step(3, "一级审批", AuditStepDef.STEP_TYPE_APPROVAL, 8L));

        assertThrows(IllegalArgumentException.class, flowDef::validate);
    }

    @Test
    void validate_shouldFailWhenSecondStepIsSubmit() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setFlowCode("PLAN_DISPATCH");
        flowDef.setFlowName("计划下发");
        flowDef.setEntityType("PLAN");
        flowDef.addStep(step(1, "提交", AuditStepDef.STEP_TYPE_SUBMIT, null));
        flowDef.addStep(step(2, "再次提交", AuditStepDef.STEP_TYPE_SUBMIT, null));

        assertThrows(IllegalArgumentException.class, flowDef::validate);
    }

    private AuditStepDef step(int order, String name, String type, Long roleId) {
        AuditStepDef step = new AuditStepDef();
        step.setStepOrder(order);
        step.setStepName(name);
        step.setStepType(type);
        step.setRoleId(roleId);
        return step;
    }
}
