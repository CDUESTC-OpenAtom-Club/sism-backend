package com.sism.workflow.application.runtime;

import com.sism.workflow.application.PlanWorkflowSyncService;
import com.sism.workflow.application.support.ApproverResolver;
import com.sism.workflow.application.support.WorkflowEventDispatcher;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.definition.repository.FlowDefinitionRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveWorkflowUseCaseTest {

    @Mock
    private AuditInstanceRepository auditInstanceRepository;

    @Mock
    private WorkflowEventDispatcher workflowEventDispatcher;

    @Mock
    private PlanWorkflowSyncService planWorkflowSyncService;

    @Mock
    private FlowDefinitionRepository flowDefinitionRepository;

    @Mock
    private ApproverResolver approverResolver;

    @InjectMocks
    private ApproveWorkflowUseCase approveWorkflowUseCase;

    @Test
    void approve_shouldCreateNextDefinitionStepAfterReturnedApprovalStep() {
        AuditFlowDef flowDef = new AuditFlowDef();

        AuditStepDef submit = new AuditStepDef();
        submit.setId(11L);
        submit.setStepName("填报人提交");
        submit.setStepOrder(1);
        submit.setStepType(AuditStepDef.STEP_TYPE_SUBMIT);

        AuditStepDef dept = new AuditStepDef();
        dept.setId(12L);
        dept.setStepName("二级学院审批人审批");
        dept.setStepOrder(2);
        dept.setStepType(AuditStepDef.STEP_TYPE_APPROVAL);
        dept.setRoleId(2L);

        AuditStepDef leader = new AuditStepDef();
        leader.setId(13L);
        leader.setStepName("学院院长审批人审批");
        leader.setStepOrder(3);
        leader.setStepType(AuditStepDef.STEP_TYPE_APPROVAL);
        leader.setRoleId(4L);
        flowDef.setSteps(List.of(submit, dept, leader));

        AuditInstance instance = new AuditInstance();
        instance.setId(1L);
        instance.setFlowDefId(4L);
        instance.setRequesterId(100L);
        instance.setRequesterOrgId(57L);
        instance.setStatus(AuditInstance.STATUS_PENDING);

        AuditStepInstance submitInstance = new AuditStepInstance();
        submitInstance.setStepNo(1);
        submitInstance.setStepDefId(11L);
        submitInstance.setStepName("填报人提交");
        submitInstance.setStatus(AuditInstance.STEP_STATUS_APPROVED);
        submitInstance.setApproverId(100L);
        submitInstance.setApproverOrgId(57L);

        AuditStepInstance rejectedLeader = new AuditStepInstance();
        rejectedLeader.setStepNo(3);
        rejectedLeader.setStepDefId(13L);
        rejectedLeader.setStepName("学院院长审批人审批");
        rejectedLeader.setStatus(AuditInstance.STEP_STATUS_REJECTED);
        rejectedLeader.setApproverId(300L);
        rejectedLeader.setApproverOrgId(57L);

        AuditStepInstance returnedDept = new AuditStepInstance();
        returnedDept.setStepNo(4);
        returnedDept.setStepDefId(12L);
        returnedDept.setStepName("二级学院审批人审批");
        returnedDept.setStatus(AuditInstance.STEP_STATUS_PENDING);
        returnedDept.setApproverId(200L);
        returnedDept.setApproverOrgId(57L);

        instance.addStepInstance(submitInstance);
        instance.addStepInstance(rejectedLeader);
        instance.addStepInstance(returnedDept);

        when(flowDefinitionRepository.findById(4L)).thenReturn(Optional.of(flowDef));
        when(approverResolver.resolveApproverId(leader, 100L, 57L)).thenReturn(300L);
        when(approverResolver.resolveApproverOrgId(300L)).thenReturn(57L);
        when(auditInstanceRepository.save(any(AuditInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditInstance saved = approveWorkflowUseCase.approve(instance, 200L, "重新通过");

        assertEquals(4, saved.getStepInstances().size());
        assertEquals(AuditInstance.STEP_STATUS_APPROVED, saved.getStepInstances().get(2).getStatus());
        assertEquals(AuditInstance.STEP_STATUS_PENDING, saved.getStepInstances().get(3).getStatus());
        assertEquals(13L, saved.getStepInstances().get(3).getStepDefId());
        assertEquals(5, saved.getStepInstances().get(3).getStepNo());
    }
}
