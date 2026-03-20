package com.sism.workflow.application;

import com.sism.workflow.application.definition.WorkflowDefinitionQueryService;
import com.sism.workflow.application.definition.WorkflowPreviewQueryService;
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

    @Mock
    private WorkflowPreviewQueryService workflowPreviewQueryService;

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
    void startWorkflowInstance_shouldForwardDefinitionAndEntityToUnifiedStartFlow() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setId(3L);
        flowDef.setFlowCode("PLAN_DISPATCH");
        flowDef.setFlowName("计划下发审批");
        flowDef.setEntityType("PLAN");
        flowDef.setIsActive(true);

        AuditInstance started = new AuditInstance();
        started.setId(30L);
        started.setFlowDefId(3L);
        started.setEntityId(88L);
        started.setStatus(AuditInstance.STATUS_PENDING);

        StartInstanceRequest request = new StartInstanceRequest();
        request.setBusinessEntityId(88L);

        when(workflowDefinitionQueryService.getAuditFlowDefById(3L)).thenReturn(flowDef);
        when(workflowDefinitionQueryService.getAuditFlowDefByCode("PLAN_DISPATCH")).thenReturn(flowDef);
        when(auditInstanceRepository.hasActiveInstance(88L, "PLAN")).thenReturn(false);
        when(workflowApplicationService.startAuditInstance(any(AuditInstance.class), any(), any()))
                .thenReturn(started);
        when(workflowReadModelMapper.toInstanceResponse(started)).thenReturn(
                WorkflowInstanceResponse.builder().instanceId("30").status("IN_REVIEW").build()
        );

        WorkflowInstanceResponse response = businessWorkflowApplicationService.startWorkflowInstance(
                "3", request, 10L, 20L);

        assertEquals("30", response.getInstanceId());

        ArgumentCaptor<AuditInstance> captor = ArgumentCaptor.forClass(AuditInstance.class);
        verify(workflowApplicationService).startAuditInstance(captor.capture(), any(), any());
        assertEquals(3L, captor.getValue().getFlowDefId());
        assertEquals(88L, captor.getValue().getEntityId());
        assertEquals("PLAN", captor.getValue().getEntityType());
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
    void approveTask_shouldResolveOwningInstanceWhenTaskIdIsStepInstanceId() {
        AuditInstance instance = new AuditInstance();
        instance.setId(128L);
        instance.setStatus(AuditInstance.STATUS_PENDING);

        com.sism.workflow.domain.runtime.model.AuditStepInstance currentStep =
                new com.sism.workflow.domain.runtime.model.AuditStepInstance();
        currentStep.setId(256L);
        currentStep.setStepIndex(2);
        currentStep.setStepName("分管校领导审批");
        currentStep.setStatus(AuditInstance.STEP_STATUS_PENDING);
        currentStep.setApproverId(9L);
        instance.addStepInstance(currentStep);

        ApprovalRequest request = new ApprovalRequest();
        request.setComment("同意");

        when(auditInstanceRepository.findById(256L)).thenReturn(Optional.empty());
        when(auditInstanceRepository.findByStepInstanceId(256L)).thenReturn(Optional.of(instance));
        when(workflowApplicationService.approveAuditInstance(instance, 9L, "同意")).thenReturn(instance);
        when(workflowReadModelMapper.toInstanceResponse(instance)).thenReturn(
                WorkflowInstanceResponse.builder().instanceId("128").status("IN_REVIEW").build()
        );

        WorkflowInstanceResponse response = businessWorkflowApplicationService.approveTask("256", request, 9L);

        assertEquals("128", response.getInstanceId());
        verify(workflowApplicationService).approveAuditInstance(instance, 9L, "同意");
    }

    @Test
    void decideTask_shouldDispatchApproveBranch() {
        WorkflowTaskDecisionRequest request = new WorkflowTaskDecisionRequest();
        request.setApproved(true);
        request.setComment("通过");

        AuditInstance instance = new AuditInstance();
        instance.setId(88L);
        instance.setStatus(AuditInstance.STATUS_PENDING);
        var currentStep = new com.sism.workflow.domain.runtime.model.AuditStepInstance();
        currentStep.setId(501L);
        currentStep.setStepIndex(2);
        currentStep.setStepName("一级审批");
        currentStep.setStatus(AuditInstance.STEP_STATUS_PENDING);
        currentStep.setApproverId(11L);
        instance.addStepInstance(currentStep);

        when(auditInstanceRepository.findById(501L)).thenReturn(Optional.empty());
        when(auditInstanceRepository.findByStepInstanceId(501L)).thenReturn(Optional.of(instance));
        when(workflowApplicationService.approveAuditInstance(instance, 11L, "通过")).thenReturn(instance);
        when(workflowReadModelMapper.toInstanceResponse(instance)).thenReturn(
                WorkflowInstanceResponse.builder().instanceId("88").status("IN_REVIEW").build()
        );

        WorkflowInstanceResponse response = businessWorkflowApplicationService.decideTask("501", request, 11L);

        assertEquals("88", response.getInstanceId());
        verify(workflowApplicationService).approveAuditInstance(instance, 11L, "通过");
    }

    @Test
    void decideTask_shouldDispatchRejectBranch() {
        WorkflowTaskDecisionRequest request = new WorkflowTaskDecisionRequest();
        request.setApproved(false);
        request.setComment("打回");

        AuditInstance instance = new AuditInstance();
        instance.setId(89L);
        instance.setStatus(AuditInstance.STATUS_PENDING);
        var currentStep = new com.sism.workflow.domain.runtime.model.AuditStepInstance();
        currentStep.setId(601L);
        currentStep.setStepIndex(2);
        currentStep.setStepName("一级审批");
        currentStep.setStatus(AuditInstance.STEP_STATUS_PENDING);
        currentStep.setApproverId(12L);
        instance.addStepInstance(currentStep);

        when(auditInstanceRepository.findById(601L)).thenReturn(Optional.empty());
        when(auditInstanceRepository.findByStepInstanceId(601L)).thenReturn(Optional.of(instance));
        when(workflowApplicationService.rejectAuditInstance(instance, 12L, "打回")).thenReturn(instance);
        when(workflowReadModelMapper.toInstanceResponse(instance)).thenReturn(
                WorkflowInstanceResponse.builder().instanceId("89").status("IN_REVIEW").build()
        );

        WorkflowInstanceResponse response = businessWorkflowApplicationService.decideTask("601", request, 12L);

        assertEquals("89", response.getInstanceId());
        verify(workflowApplicationService).rejectAuditInstance(instance, 12L, "打回");
    }

    @Test
    void listDefinitions_shouldMapPageResults() {
        WorkflowDefinitionResponse item = WorkflowDefinitionResponse.builder()
                .definitionId("1")
                .definitionCode("REPORT_APPROVAL")
                .definitionName("报告审批")
                .build();
        PageResult<WorkflowDefinitionResponse> expected = PageResult.of(List.of(item), 1, 1, 10);
        when(workflowReadModelService.listDefinitions(1, 10)).thenReturn(expected);

        PageResult<WorkflowDefinitionResponse> result = businessWorkflowApplicationService.listDefinitions(1, 10);

        assertEquals(1, result.getTotal());
        assertEquals("报告审批", result.getItems().get(0).getDefinitionName());
    }

    @Test
    void createDefinition_shouldMapTemplateWithExplicitSteps() {
        CreateWorkflowDefinitionRequest request = new CreateWorkflowDefinitionRequest();
        request.setDefinitionCode("PLAN_DISPATCH");
        request.setDefinitionName("计划下发审批");
        request.setCategory("PLAN");
        request.setDescription("固定模板");
        request.setActive(true);

        CreateWorkflowStepDefinitionRequest submitStep = new CreateWorkflowStepDefinitionRequest();
        submitStep.setStepName("提交");
        submitStep.setStepOrder(1);
        submitStep.setStepType("SUBMIT");

        CreateWorkflowStepDefinitionRequest approvalStep = new CreateWorkflowStepDefinitionRequest();
        approvalStep.setStepName("一级审批");
        approvalStep.setStepOrder(2);
        approvalStep.setStepType("APPROVAL");
        approvalStep.setRoleId(8L);

        request.setSteps(List.of(submitStep, approvalStep));

        AuditFlowDef created = new AuditFlowDef();
        created.setId(10L);
        created.setFlowCode("PLAN_DISPATCH");
        created.setFlowName("计划下发审批");
        created.setEntityType("PLAN");
        created.setIsActive(true);

        when(workflowDefinitionQueryService.createAuditFlowDef(any(AuditFlowDef.class))).thenReturn(created);
        when(workflowReadModelMapper.toDefinitionResponse(created)).thenReturn(
                WorkflowDefinitionResponse.builder()
                        .definitionId("10")
                        .definitionCode("PLAN_DISPATCH")
                        .definitionName("计划下发审批")
                        .build()
        );

        WorkflowDefinitionResponse response = businessWorkflowApplicationService.createDefinition(request);

        assertEquals("10", response.getDefinitionId());
        assertEquals("PLAN_DISPATCH", response.getDefinitionCode());

        ArgumentCaptor<AuditFlowDef> captor = ArgumentCaptor.forClass(AuditFlowDef.class);
        verify(workflowDefinitionQueryService).createAuditFlowDef(captor.capture());
        assertEquals("PLAN_DISPATCH", captor.getValue().getFlowCode());
        assertEquals(2, captor.getValue().getSteps().size());
        assertEquals("SUBMIT", captor.getValue().getSteps().get(0).getStepType());
        assertEquals("APPROVAL", captor.getValue().getSteps().get(1).getStepType());
    }

    @Test
    void getInstanceDetailByBusiness_shouldIgnoreLatestWithdrawnInstance() {
        AuditInstance withdrawn = new AuditInstance();
        withdrawn.setId(301L);
        withdrawn.setEntityType("PLAN");
        withdrawn.setEntityId(77L);
        withdrawn.setStatus(AuditInstance.STATUS_WITHDRAWN);
        withdrawn.setStartedAt(java.time.LocalDateTime.of(2026, 3, 20, 10, 0));

        AuditInstance approved = new AuditInstance();
        approved.setId(300L);
        approved.setEntityType("PLAN");
        approved.setEntityId(77L);
        approved.setStatus(AuditInstance.STATUS_APPROVED);
        approved.setStartedAt(java.time.LocalDateTime.of(2026, 3, 19, 10, 0));

        when(auditInstanceRepository.findByBusinessTypeAndBusinessId("PLAN", 77L))
                .thenReturn(List.of(approved, withdrawn));

        assertThrows(
                IllegalArgumentException.class,
                () -> businessWorkflowApplicationService.getInstanceDetailByBusiness("PLAN", 77L)
        );
    }
}
