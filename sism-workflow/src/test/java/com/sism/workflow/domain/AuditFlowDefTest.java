package com.sism.workflow.domain;

import com.sism.shared.domain.model.workflow.AuditFlowDef;
import com.sism.shared.domain.model.workflow.AuditStepDef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditFlowDef Aggregate Root Tests")
class AuditFlowDefTest {

    @Test
    @DisplayName("Should create AuditFlowDef with valid parameters")
    void shouldCreateAuditFlowDefWithValidParameters() {
        AuditFlowDef flowDef = buildFlowDef();

        assertNotNull(flowDef);
        assertEquals("FLOW_001", flowDef.getFlowCode());
        assertEquals("测试流程", flowDef.getFlowName());
        assertEquals("INDICATOR", flowDef.getEntityType());
        assertTrue(flowDef.getIsActive());
    }

    @Test
    @DisplayName("Should throw exception when creating AuditFlowDef with null flow code")
    void shouldThrowExceptionWhenCreatingAuditFlowDefWithNullFlowCode() {
        AuditFlowDef flowDef = buildFlowDef();
        flowDef.setFlowCode(null);

        assertThrows(IllegalArgumentException.class, flowDef::validate);
    }

    @Test
    @DisplayName("Should add step to AuditFlowDef successfully")
    void shouldAddStepToAuditFlowDefSuccessfully() {
        AuditFlowDef flowDef = buildFlowDef();
        AuditStepDef step = new AuditStepDef();
        step.setStepName("审核步骤");
        step.setStepOrder(1);
        step.setApproverType("ROLE");
        step.setApproverId(1L);

        flowDef.addStep(step);

        assertEquals(1, flowDef.getStepCount());
        assertTrue(flowDef.getSteps().contains(step));
    }

    @Test
    @DisplayName("Should validate AuditFlowDef with valid parameters")
    void shouldValidateAuditFlowDefWithValidParameters() {
        AuditFlowDef flowDef = buildFlowDef();
        flowDef.setFlowName("有效流程");

        assertDoesNotThrow(flowDef::validate);
    }

    private AuditFlowDef buildFlowDef() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setFlowCode("FLOW_001");
        flowDef.setFlowName("测试流程");
        flowDef.setEntityType("INDICATOR");
        flowDef.setIsActive(true);
        flowDef.setVersion(1);
        return flowDef;
    }
}
