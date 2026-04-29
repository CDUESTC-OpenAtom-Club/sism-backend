package com.sism.workflow.interfaces.rest;

import com.sism.workflow.application.definition.WorkflowDefinitionQueryService;
import com.sism.workflow.domain.definition.AuditFlowDef;
import com.sism.workflow.domain.definition.AuditStepDef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalFlowCompatibilityController tests")
class ApprovalFlowCompatibilityControllerTest {

    @Mock
    private WorkflowDefinitionQueryService workflowDefinitionQueryService;

    @Test
    @DisplayName("listFlows should map and sort workflow steps")
    void listFlowsShouldMapAndSortWorkflowSteps() {
        ApprovalFlowCompatibilityController controller = new ApprovalFlowCompatibilityController(workflowDefinitionQueryService);

        AuditStepDef later = new AuditStepDef();
        later.setId(12L);
        later.setStepName("二级审批");
        later.setStepOrder(2);
        later.setStepType(AuditStepDef.STEP_TYPE_APPROVAL);
        later.setRoleId(8L);
        later.setIsRequired(true);

        AuditStepDef first = new AuditStepDef();
        first.setId(11L);
        first.setStepName("提交");
        first.setStepOrder(1);
        first.setStepType(AuditStepDef.STEP_TYPE_SUBMIT);

        AuditFlowDef flow = new AuditFlowDef();
        flow.setId(1L);
        flow.setFlowCode("FLOW-1");
        flow.setFlowName("Flow One");
        flow.setEntityType("PLAN");
        flow.setIsActive(true);
        flow.setSteps(List.of(later, first));

        when(workflowDefinitionQueryService.getAllAuditFlowDefs()).thenReturn(List.of(flow));

        var response = controller.listFlows();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("FLOW-1", response.getBody().getData().get(0).getFlowCode());
        assertEquals(2, response.getBody().getData().get(0).getStepCount());
        assertEquals("提交", response.getBody().getData().get(0).getSteps().get(0).getStepName());
        assertEquals("二级审批", response.getBody().getData().get(0).getSteps().get(1).getStepName());
    }

    @Test
    @DisplayName("getFlowById should throw when definition is missing")
    void getFlowByIdShouldThrowWhenDefinitionMissing() {
        ApprovalFlowCompatibilityController controller = new ApprovalFlowCompatibilityController(workflowDefinitionQueryService);

        when(workflowDefinitionQueryService.getAuditFlowDefById(9L)).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> controller.getFlowById(9L));
        assertEquals("Workflow definition not found: 9", error.getMessage());
    }
}
