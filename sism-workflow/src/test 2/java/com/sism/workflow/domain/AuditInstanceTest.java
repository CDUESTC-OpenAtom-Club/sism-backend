package com.sism.workflow.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditInstance Aggregate Root Tests")
class AuditInstanceTest {

    @Test
    @DisplayName("Should create AuditInstance with valid parameters")
    void shouldCreateAuditInstanceWithValidParameters() {
        AuditFlowDef flowDef = AuditFlowDef.create("FLOW_001", "测试流程", "INDICATOR");

        AuditInstance instance = AuditInstance.create("测试实例", 1L, "INDICATOR", flowDef);

        assertNotNull(instance);
        assertEquals("测试实例", instance.getTitle());
        assertEquals(1L, instance.getEntityId());
        assertEquals("INDICATOR", instance.getEntityType());
        assertEquals(AuditInstance.STATUS_PENDING, instance.getStatus());
        assertNotNull(instance.getStartedAt());
    }

    @Test
    @DisplayName("Should throw exception when creating AuditInstance with null title")
    void shouldThrowExceptionWhenCreatingAuditInstanceWithNullTitle() {
        AuditFlowDef flowDef = AuditFlowDef.create("FLOW_001", "测试流程", "INDICATOR");

        assertThrows(IllegalArgumentException.class, () ->
            AuditInstance.create(null, 1L, "INDICATOR", flowDef)
        );
    }

    @Test
    @DisplayName("Should throw exception when creating AuditInstance with invalid entity id")
    void shouldThrowExceptionWhenCreatingAuditInstanceWithInvalidEntityId() {
        AuditFlowDef flowDef = AuditFlowDef.create("FLOW_001", "测试流程", "INDICATOR");

        assertThrows(IllegalArgumentException.class, () ->
            AuditInstance.create("测试实例", -1L, "INDICATOR", flowDef)
        );
    }

    @Test
    @DisplayName("Should validate AuditInstance with valid parameters")
    void shouldValidateAuditInstanceWithValidParameters() {
        AuditFlowDef flowDef = AuditFlowDef.create("FLOW_001", "测试流程", "INDICATOR");
        AuditInstance instance = AuditInstance.create("有效实例", 1L, "INDICATOR", flowDef);

        assertDoesNotThrow(instance::validate);
    }
}
