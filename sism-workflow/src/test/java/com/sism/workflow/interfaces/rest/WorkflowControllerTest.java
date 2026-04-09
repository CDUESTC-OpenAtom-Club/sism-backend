package com.sism.workflow.interfaces.rest;

import com.sism.workflow.application.WorkflowApplicationService;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowController tests")
class WorkflowControllerTest {

    @Mock
    private WorkflowApplicationService workflowApplicationService;

    @Test
    @DisplayName("getFlowDefinitionById should return 404 payload when missing")
    void getFlowDefinitionByIdShouldReturn404PayloadWhenMissing() {
        WorkflowController controller = new WorkflowController(workflowApplicationService);

        when(workflowApplicationService.getAuditFlowDefById(9L)).thenReturn(null);

        var response = controller.getFlowDefinitionById(9L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(404, response.getBody().getCode());
    }

    @Test
    @DisplayName("createFlowDefinition should wrap created definition")
    void createFlowDefinitionShouldWrapCreatedDefinition() {
        WorkflowController controller = new WorkflowController(workflowApplicationService);
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setFlowCode("FLOW-1");

        when(workflowApplicationService.createAuditFlowDef(flowDef)).thenReturn(flowDef);

        var response = controller.createFlowDefinition(flowDef);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(flowDef, response.getBody().getData());
    }

    @Test
    @DisplayName("startInstance should pass requester identity to service")
    void startInstanceShouldPassRequesterIdentityToService() {
        WorkflowController controller = new WorkflowController(workflowApplicationService);
        AuditInstance request = new AuditInstance();
        AuditInstance started = new AuditInstance();
        started.setId(11L);

        when(workflowApplicationService.startAuditInstance(request, 91L, 38L)).thenReturn(started);

        var response = controller.startInstance(request, 91L, 38L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(11L, response.getBody().getData().getId());
        verify(workflowApplicationService).startAuditInstance(request, 91L, 38L);
    }

    @Test
    @DisplayName("cancelInstance should return 404 payload when instance is missing")
    void cancelInstanceShouldReturn404PayloadWhenInstanceMissing() {
        WorkflowController controller = new WorkflowController(workflowApplicationService);

        when(workflowApplicationService.getAuditInstanceById(10L)).thenReturn(null);

        var response = controller.cancelInstance(10L, new AuditInstance());

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(404, response.getBody().getCode());
    }
}
