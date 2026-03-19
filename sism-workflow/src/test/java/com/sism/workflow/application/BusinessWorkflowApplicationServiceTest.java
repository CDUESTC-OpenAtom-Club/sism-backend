package com.sism.workflow.application;

import com.sism.workflow.application.definition.WorkflowDefinitionQueryService;
import com.sism.workflow.application.query.WorkflowReadModelMapper;
import com.sism.workflow.application.query.WorkflowReadModelService;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.query.repository.WorkflowQueryRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import com.sism.workflow.interfaces.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessWorkflowApplicationServiceTest {

    @Mock
    private WorkflowDefinitionQueryService workflowDefinitionQueryService;

    @Mock
    private AuditInstanceRepository auditInstanceRepository;

    @Mock
    private WorkflowQueryRepository workflowQueryRepository;

    @Mock
    private WorkflowApplicationService workflowApplicationService;

    @Mock
    private WorkflowReadModelService workflowReadModelService;

    @Mock
    private WorkflowReadModelMapper workflowReadModelMapper;

    @InjectMocks
    private BusinessWorkflowApplicationService businessWorkflowApplicationService;

    @Test
    void startWorkflow_shouldReturnStartedInstance() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setId(1L);
        flowDef.setFlowCode("REPORT_APPROVAL");
        flowDef.setFlowName("报告审批");
        flowDef.setEntityType("PlanReport");
        flowDef.setIsActive(true);

        AuditInstance started = new AuditInstance();
        started.setId(9L);
        started.setFlowDefId(1L);
        started.setEntityId(18L);
        started.setStatus(AuditInstance.STATUS_PENDING);

        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setWorkflowCode("REPORT_APPROVAL");
        request.setBusinessEntityId(18L);
        request.setBusinessEntityType("PlanReport");

        when(workflowDefinitionQueryService.getAuditFlowDefByCode("REPORT_APPROVAL")).thenReturn(flowDef);
        when(auditInstanceRepository.hasActiveInstance(18L, "PlanReport")).thenReturn(false);
        when(workflowApplicationService.startAuditInstance(any(AuditInstance.class), any(), any())).thenReturn(started);
        when(workflowReadModelMapper.toInstanceResponse(started)).thenReturn(
                WorkflowInstanceResponse.builder().instanceId("9").status("IN_REVIEW").build()
        );

        WorkflowInstanceResponse response = businessWorkflowApplicationService.startWorkflow(request, 1L, 2L);

        ArgumentCaptor<AuditInstance> captor = ArgumentCaptor.forClass(AuditInstance.class);
        verify(workflowApplicationService).startAuditInstance(captor.capture(), any(), any());

        assertEquals("9", response.getInstanceId());
        assertEquals("IN_REVIEW", response.getStatus());
        assertEquals(1L, captor.getValue().getFlowDefId());
        assertEquals("PlanReport", captor.getValue().getEntityType());
        assertEquals(18L, captor.getValue().getEntityId());
    }

    @Test
    void approveTask_shouldRejectUnauthorizedUser() {
        AuditInstance instance = new AuditInstance();
        instance.setId(1L);
        instance.setStepInstances(List.of());

        ApprovalRequest request = new ApprovalRequest();
        when(auditInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThrows(SecurityException.class, () -> businessWorkflowApplicationService.approveTask("1", request, 7L));
    }

    @Test
    void listDefinitions_shouldMapPageResults() {
        WorkflowDefinitionResponse item = WorkflowDefinitionResponse.builder()
                .definitionId("1")
                .definitionName("报告审批")
                .build();
        PageResult<WorkflowDefinitionResponse> expected = PageResult.of(List.of(item), 1, 1, 10);
        when(workflowReadModelService.listDefinitions(1, 10)).thenReturn(expected);

        PageResult<WorkflowDefinitionResponse> result = businessWorkflowApplicationService.listDefinitions(1, 10);

        assertEquals(1, result.getTotal());
        assertEquals("报告审批", result.getItems().get(0).getDefinitionName());
    }
}
