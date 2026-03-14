package com.sism.workflow.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditFlowDef Aggregate Root Tests")
class AuditFlowDefTest {

    @Test
    @DisplayName("Should create AuditFlowDef with valid parameters")
    void shouldCreateAuditFlowDefWithValidParameters() {
        AuditFlowDef flowDef = AuditFlowDef.create("FLOW_001", "测试流程", "INDICATOR");

        assertNotNull(flowDef);
        assertEquals("FLOW_001", flowDef.getFlowCode());
        assertEquals("测试流程", flowDef.getName());
        assertEquals("INDICATOR", flowDef.getEntityType());
        assertTrue(flowDef.getIsActive());
        assertNotNull(flowDef.getCreatedAt());
        assertNotNull(flowDef.getUpdatedAt());
    }

    @Test
    @DisplayName("Should throw exception when creating AuditFlowDef with null flow code")
    void shouldThrowExceptionWhenCreatingAuditFlowDefWithNullFlowCode() {
        assertThrows(IllegalArgumentException.class, () ->
            AuditFlowDef.create(null, "测试流程", "INDICATOR")
        );
    }

    @Test
    @DisplayName("Should add step to AuditFlowDef successfully")
    void shouldAddStepToAuditFlowDefSuccessfully() {
        AuditFlowDef flowDef = AuditFlowDef.create("FLOW_001", "测试流程", "INDICATOR");
        AuditStepDef step = AuditStepDef.create("审核步骤", "部门负责人审核");

        flowDef.addStep(step);

        assertEquals(1, flowDef.getStepCount());
        assertTrue(flowDef.getSteps().contains(step));
    }

    @Test
    @DisplayName("Should validate AuditFlowDef with valid parameters")
    void shouldValidateAuditFlowDefWithValidParameters() {
        AuditFlowDef flowDef = AuditFlowDef.create("FLOW_001", "有效流程", "INDICATOR");

        assertDoesNotThrow(flowDef::validate);
    }
}
