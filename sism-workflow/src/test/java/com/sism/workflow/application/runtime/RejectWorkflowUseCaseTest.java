package com.sism.workflow.application.runtime;

import com.sism.workflow.application.WorkflowBusinessStatusSyncService;
import com.sism.workflow.application.definition.WorkflowDefinitionQueryService;
import com.sism.workflow.application.support.WorkflowEventDispatcher;
import com.sism.workflow.domain.definition.AuditFlowDef;
import com.sism.workflow.domain.definition.AuditStepDef;
import com.sism.workflow.domain.runtime.AuditInstance;
import com.sism.workflow.domain.runtime.AuditStepInstance;
import com.sism.workflow.domain.runtime.AuditInstanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RejectWorkflowUseCaseTest {

    @Mock
    private AuditInstanceRepository auditInstanceRepository;

    @Mock
    private WorkflowEventDispatcher workflowEventDispatcher;

    @Mock
    private WorkflowBusinessStatusSyncService workflowBusinessStatusSyncService;

    @Mock
    private WorkflowDefinitionQueryService workflowDefinitionQueryService;

    @InjectMocks
    private RejectWorkflowUseCase rejectWorkflowUseCase;

    @Test
    void reject_shouldKeepInstanceInReviewWhenReturnedToSubmitStep() {
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
        dept.setIsTerminal(true);

        flowDef.setSteps(List.of(submit, dept));

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

        AuditStepInstance deptInstance = new AuditStepInstance();
        deptInstance.setStepNo(2);
        deptInstance.setStepDefId(12L);
        deptInstance.setStepName("二级学院审批人审批");
        deptInstance.setStatus(AuditInstance.STEP_STATUS_PENDING);
        deptInstance.setApproverId(300L);
        deptInstance.setApproverOrgId(57L);

        instance.addStepInstance(submitInstance);
        instance.addStepInstance(deptInstance);

        when(workflowDefinitionQueryService.getAuditFlowDefById(4L)).thenReturn(flowDef);
        when(auditInstanceRepository.save(any(AuditInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditInstance saved = rejectWorkflowUseCase.reject(instance, 300L, "打回");

        assertEquals(AuditInstance.STATUS_WITHDRAWN, saved.getStatus());
        assertEquals(3, saved.getStepInstances().size());
        assertEquals(AuditInstance.STEP_STATUS_REJECTED, saved.getStepInstances().get(1).getStatus());
        assertEquals(AuditInstance.STEP_STATUS_WITHDRAWN, saved.getStepInstances().get(2).getStatus());
        assertEquals(11L, saved.getStepInstances().get(2).getStepDefId());
        assertEquals(3, saved.getStepInstances().get(2).getStepNo());
        assertEquals(100L, saved.getStepInstances().get(2).getApproverId());
        assertEquals("驳回后退回填报人重新提交", saved.getStepInstances().get(2).getComment());
    }

    @Test
    void reject_shouldMarkInstanceRejectedWhenRejectingTerminalFirstStep() {
        AuditFlowDef flowDef = new AuditFlowDef();

        AuditStepDef submit = new AuditStepDef();
        submit.setId(11L);
        submit.setStepName("填报人提交");
        submit.setStepOrder(1);
        submit.setStepType(AuditStepDef.STEP_TYPE_SUBMIT);
        submit.setIsTerminal(true);

        flowDef.setSteps(List.of(submit));

        AuditInstance instance = new AuditInstance();
        instance.setId(2L);
        instance.setFlowDefId(5L);
        instance.setRequesterId(100L);
        instance.setRequesterOrgId(57L);
        instance.setStatus(AuditInstance.STATUS_PENDING);

        AuditStepInstance submitInstance = new AuditStepInstance();
        submitInstance.setStepNo(1);
        submitInstance.setStepDefId(11L);
        submitInstance.setStepName("填报人提交");
        submitInstance.setStatus(AuditInstance.STEP_STATUS_PENDING);
        submitInstance.setApproverOrgId(57L);

        instance.addStepInstance(submitInstance);

        when(workflowDefinitionQueryService.getAuditFlowDefById(5L)).thenReturn(flowDef);
        when(auditInstanceRepository.save(any(AuditInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditInstance saved = rejectWorkflowUseCase.reject(instance, 100L, "终止");

        assertEquals(AuditInstance.STATUS_REJECTED, saved.getStatus());
        assertEquals(1, saved.getStepInstances().size());
        assertEquals(AuditInstance.STEP_STATUS_REJECTED, saved.getStepInstances().get(0).getStatus());
    }
}
