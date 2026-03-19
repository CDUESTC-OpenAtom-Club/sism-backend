package com.sism.workflow.application;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.shared.domain.model.workflow.AuditFlowDef;
import com.sism.shared.domain.model.workflow.AuditInstance;
import com.sism.shared.domain.model.workflow.AuditStepDef;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.workflow.domain.repository.WorkflowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowApplicationServiceTest {

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private EventStore eventStore;

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkflowApplicationService workflowApplicationService;

    @Test
    void startAuditInstance_shouldAutoCompleteSubmitterStepAndActivateNextApprover() {
        AuditStepDef submitStep = new AuditStepDef();
        submitStep.setId(1L);
        submitStep.setStepOrder(1);
        submitStep.setStepName("填报人提交");

        AuditStepDef approvalStep = new AuditStepDef();
        approvalStep.setId(2L);
        approvalStep.setStepOrder(2);
        approvalStep.setStepName("战略发展部负责人审批");
        approvalStep.setApproverType("ROLE");
        approvalStep.setApproverId(10L);

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

        User requester = new User();
        requester.setId(188L);
        requester.setUsername("zlb_admin");
        requester.setRealName("战略部管理员");
        requester.setOrgId(35L);
        requester.setIsActive(true);

        when(workflowRepository.findAuditFlowDefById(1L)).thenReturn(Optional.of(flowDef));
        when(userRepository.findByRoleId(10L)).thenReturn(List.of(approver));
        when(userRepository.findById(188L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(189L)).thenReturn(Optional.of(approver));
        when(workflowRepository.saveAuditInstance(any(AuditInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditInstance instance = new AuditInstance();
        instance.setFlowDefId(1L);
        instance.setEntityType("TASK");
        instance.setEntityId(30058L);
        instance.setTitle("测试流程");

        AuditInstance result = workflowApplicationService.startAuditInstance(instance, 188L, 35L);

        assertEquals(AuditInstance.STATUS_PENDING, result.getStatus());
        assertEquals(2, result.getCurrentStepIndex());
        assertEquals(2, result.getStepInstances().size());

        var firstStep = result.getStepInstances().get(0);
        assertEquals(AuditInstance.STEP_STATUS_APPROVED, firstStep.getStatus());
        assertEquals(188L, firstStep.getApproverId());
        assertNotNull(firstStep.getApprovedAt());

        var secondStep = result.getStepInstances().get(1);
        assertEquals(AuditInstance.STEP_STATUS_PENDING, secondStep.getStatus());
        assertEquals(189L, secondStep.getApproverId());
        assertEquals("战略部审核人1", secondStep.getApproverName());
    }
}
