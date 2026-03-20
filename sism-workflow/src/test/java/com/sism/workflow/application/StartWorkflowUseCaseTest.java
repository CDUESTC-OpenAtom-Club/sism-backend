package com.sism.workflow.application;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.workflow.application.runtime.StartWorkflowUseCase;
import com.sism.workflow.application.support.ApproverResolver;
import com.sism.workflow.application.support.FlowResolver;
import com.sism.workflow.application.support.StepInstanceFactory;
import com.sism.workflow.application.support.SubmissionStepAutoCompletePolicy;
import com.sism.workflow.application.support.WorkflowEventDispatcher;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.definition.repository.FlowDefinitionRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartWorkflowUseCaseTest {

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private EventStore eventStore;

    @Mock
    private FlowDefinitionRepository flowDefinitionRepository;

    @Mock
    private AuditInstanceRepository auditInstanceRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void startAuditInstance_shouldAutoCompleteSubmitterStepAndActivateNextApprover() {
        AuditStepDef submitStep = new AuditStepDef();
        submitStep.setId(1L);
        submitStep.setStepOrder(1);
        submitStep.setStepName("填报人提交");
        submitStep.setStepType(AuditStepDef.STEP_TYPE_SUBMIT);

        AuditStepDef approvalStep = new AuditStepDef();
        approvalStep.setId(2L);
        approvalStep.setStepOrder(2);
        approvalStep.setStepName("战略发展部负责人审批");
        approvalStep.setStepType(AuditStepDef.STEP_TYPE_APPROVAL);
        approvalStep.setRoleId(10L);

        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setId(1L);
        flowDef.setFlowName("Plan下发审批（战略发展部）");
        flowDef.setSteps(List.of(submitStep, approvalStep));

        User approver = new User();
        approver.setId(189L);
        approver.setUsername("zlb_audit1");
        approver.setRealName("战略部审核人1");
        approver.setOrgId(35L);
        approver.setIsActive(true);

        when(flowDefinitionRepository.findById(1L)).thenReturn(Optional.of(flowDef));
        when(userRepository.findByRoleId(10L)).thenReturn(List.of(approver));
        when(auditInstanceRepository.save(any(AuditInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(flowDefinitionRepository.findByCode(any())).thenReturn(Optional.empty());
        lenient().when(flowDefinitionRepository.findByEntityType(any())).thenReturn(List.of());

        FlowResolver flowResolver = new FlowResolver(flowDefinitionRepository);
        ApproverResolver approverResolver = new ApproverResolver(userRepository);
        SubmissionStepAutoCompletePolicy autoCompletePolicy = new SubmissionStepAutoCompletePolicy();
        StepInstanceFactory stepInstanceFactory = new StepInstanceFactory(approverResolver, autoCompletePolicy);
        WorkflowEventDispatcher workflowEventDispatcher = new WorkflowEventDispatcher(eventPublisher, eventStore);
        StartWorkflowUseCase useCase = new StartWorkflowUseCase(
                flowResolver,
                flowDefinitionRepository,
                auditInstanceRepository,
                stepInstanceFactory,
                workflowEventDispatcher
        );

        AuditInstance instance = new AuditInstance();
        instance.setFlowDefId(1L);
        instance.setEntityType("TASK");
        instance.setEntityId(30058L);
        AuditInstance result = useCase.startAuditInstance(instance, 188L, 35L, java.util.Map.of());

        assertEquals(AuditInstance.STATUS_PENDING, result.getStatus());
        assertEquals(2, result.getStepInstances().size());

        var firstStep = result.getStepInstances().get(0);
        assertEquals(AuditInstance.STEP_STATUS_APPROVED, firstStep.getStatus());
        assertEquals(188L, firstStep.getApproverId());
        assertNotNull(firstStep.getApprovedAt());

        var secondStep = result.getStepInstances().get(1);
        assertEquals(AuditInstance.STEP_STATUS_PENDING, secondStep.getStatus());
        assertEquals(189L, secondStep.getApproverId());
        assertEquals(2, result.resolveCurrentPendingStep().orElseThrow().getStepIndex());
    }
}
