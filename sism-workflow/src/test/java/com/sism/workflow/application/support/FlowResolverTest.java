package com.sism.workflow.application.support;

import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.repository.FlowDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlowResolverTest {

    @Mock
    private FlowDefinitionRepository flowDefinitionRepository;

    @Test
    void findFlowByPreferredCode_shouldMatchEnabledFlowByCode() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setId(1L);
        flowDef.setFlowCode("PLAN_DISPATCH_STRATEGY");
        flowDef.setEntityType("INDICATOR");
        flowDef.setIsActive(true);

        when(flowDefinitionRepository.findByCode("PLAN_DISPATCH_STRATEGY")).thenReturn(Optional.of(flowDef));

        FlowResolver resolver = new FlowResolver(flowDefinitionRepository);
        AuditFlowDef resolved = resolver.findFlowByPreferredCode("TASK").orElse(null);

        assertEquals(1L, resolved.getId());
    }

    @Test
    void resolveAndAttachFlow_shouldFallbackToEntityTypeWhenPreferredCodeMissing() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setId(2L);
        flowDef.setEntityType("PLAN_REPORT");
        flowDef.setIsActive(true);

        when(flowDefinitionRepository.findByCode("INDICATOR_DEFAULT_APPROVAL")).thenReturn(Optional.empty());
        when(flowDefinitionRepository.findByCode("PLAN_DISPATCH_STRATEGY")).thenReturn(Optional.empty());
        when(flowDefinitionRepository.findByCode("PLAN_DISPATCH_FUNCDEPT")).thenReturn(Optional.empty());
        when(flowDefinitionRepository.findByEntityType("PLAN_REPORT")).thenReturn(List.of(flowDef));

        FlowResolver resolver = new FlowResolver(flowDefinitionRepository);
        var instance = new com.sism.workflow.domain.runtime.model.AuditInstance();
        instance.setEntityType("PLAN_REPORT");
        instance.setEntityId(10L);

        resolver.resolveAndAttachFlow(instance);

        assertEquals(2L, instance.getFlowDefId());
    }

    @Test
    void findFlowByPreferredCode_shouldIgnoreDisabledFlow() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setId(3L);
        flowDef.setFlowCode("PLAN_DISPATCH_STRATEGY");
        flowDef.setEntityType("INDICATOR");
        flowDef.setIsActive(false);

        when(flowDefinitionRepository.findByCode("PLAN_DISPATCH_STRATEGY")).thenReturn(Optional.of(flowDef));
        when(flowDefinitionRepository.findByCode("PLAN_DISPATCH_FUNCDEPT")).thenReturn(Optional.empty());
        when(flowDefinitionRepository.findByCode("INDICATOR_DEFAULT_APPROVAL")).thenReturn(Optional.empty());

        FlowResolver resolver = new FlowResolver(flowDefinitionRepository);

        assertNull(resolver.findFlowByPreferredCode("TASK").orElse(null));
    }
}
