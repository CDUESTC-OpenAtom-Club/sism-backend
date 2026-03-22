package com.sism.workflow.application.query;

import com.sism.workflow.application.definition.WorkflowDefinitionQueryService;
import com.sism.workflow.application.support.ApproverResolver;
import com.sism.iam.domain.Role;
import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.query.repository.WorkflowQueryRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import com.sism.workflow.interfaces.dto.PageResult;
import com.sism.workflow.interfaces.dto.WorkflowTaskResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowReadModelServiceTest {

    @Mock
    private WorkflowDefinitionQueryService workflowDefinitionQueryService;

    @Mock
    private AuditInstanceRepository auditInstanceRepository;

    @Mock
    private WorkflowQueryRepository workflowQueryRepository;

    @Mock
    private UserRepository userRepository;

    private WorkflowReadModelService newService() {
        WorkflowReadModelMapper mapper = new WorkflowReadModelMapper(new ApproverResolver(userRepository));
        return new WorkflowReadModelService(
                workflowDefinitionQueryService,
                auditInstanceRepository,
                workflowQueryRepository,
                mapper,
                new ApproverResolver(userRepository)
        );
    }

    @Test
    void listInstances_shouldMapInstancePage() {
        AuditInstance instance = new AuditInstance();
        instance.setId(7L);
        instance.setFlowDefId(3L);
        instance.setEntityId(99L);
        instance.setStatus(AuditInstance.STATUS_PENDING);

        when(auditInstanceRepository.findByFlowDefId(3L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(instance), PageRequest.of(0, 10), 1));

        PageResult<?> result = newService().listInstances("3", 1, 10);

        assertEquals(1, result.getTotal());
    }

    @Test
    void getMyPendingTasks_shouldReturnStepLevelTaskView() {
        AuditInstance instance = new AuditInstance();
        instance.setId(8L);
        instance.setFlowDefId(2L);
        instance.setEntityType("PlanReport");
        instance.setEntityId(88L);
        instance.setRequesterOrgId(44L);
        instance.setStartedAt(LocalDateTime.now().minusHours(1));

        AuditStepInstance pending = new AuditStepInstance();
        pending.setId(88L);
        pending.setStepDefId(5L);
        pending.setStepNo(2);
        pending.setStepName("部门负责人审批");
        pending.setStatus(AuditInstance.STEP_STATUS_PENDING);
        pending.setApproverId(101L);
        pending.setCreatedAt(LocalDateTime.now());

        instance.addStepInstance(pending);

        AuditFlowDef flowDef = new AuditFlowDef();
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setId(5L);
        stepDef.setRoleId(6L);
        flowDef.setSteps(List.of(stepDef));

        User approver = new User();
        approver.setId(101L);
        approver.setRealName("审批人A");
        approver.setIsActive(true);
        approver.setOrgId(44L);
        Role role = new Role();
        role.setId(6L);
        approver.setRoles(java.util.Set.of(role));
        when(userRepository.findById(101L)).thenReturn(Optional.of(approver));
        when(userRepository.findRoleIdsByUserId(101L)).thenReturn(List.of(6L));
        when(auditInstanceRepository.findByStatus(AuditInstance.STATUS_PENDING)).thenReturn(List.of(instance));
        when(workflowDefinitionQueryService.getAuditFlowDefById(2L)).thenReturn(flowDef);

        PageResult<WorkflowTaskResponse> result = newService().getMyPendingTasks(101L, 1);

        assertEquals(1, result.getTotal());
        assertEquals("88", result.getItems().get(0).getTaskId());
        assertEquals("step_5", result.getItems().get(0).getTaskKey());
        assertEquals("审批人A", result.getItems().get(0).getAssigneeName());
    }

    @Test
    void getMyPendingTasks_shouldFallbackTaskNameToEntityLabelWhenStepNameMissing() {
        AuditInstance instance = new AuditInstance();
        instance.setId(18L);
        instance.setFlowDefId(1L);
        instance.setEntityType("PlanReport");
        instance.setEntityId(123L);
        instance.setRequesterOrgId(35L);
        instance.setStartedAt(LocalDateTime.now().minusHours(2));

        AuditStepInstance pending = new AuditStepInstance();
        pending.setId(180L);
        pending.setStepDefId(2L);
        pending.setStepNo(1);
        pending.setStatus(AuditInstance.STEP_STATUS_PENDING);
        pending.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        instance.addStepInstance(pending);

        AuditFlowDef flowDef = new AuditFlowDef();
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setId(2L);
        stepDef.setRoleId(8L);
        flowDef.setSteps(List.of(stepDef));

        User approver = new User();
        approver.setId(201L);
        approver.setUsername("approver201");
        approver.setIsActive(true);
        approver.setOrgId(35L);
        Role role = new Role();
        role.setId(8L);
        approver.setRoles(java.util.Set.of(role));
        when(userRepository.findById(201L)).thenReturn(Optional.of(approver));
        when(userRepository.findRoleIdsByUserId(201L)).thenReturn(List.of(8L));
        when(auditInstanceRepository.findByStatus(AuditInstance.STATUS_PENDING)).thenReturn(List.of(instance));
        when(workflowDefinitionQueryService.getAuditFlowDefById(1L)).thenReturn(flowDef);

        PageResult<WorkflowTaskResponse> result = newService().getMyPendingTasks(201L, 1);

        assertEquals(1, result.getTotal());
        assertEquals("PlanReport#123", result.getItems().get(0).getTaskName());
        assertEquals("Unknown", result.getItems().get(0).getAssigneeName());
    }

    @Test
    void getInstanceDetail_shouldBuildHistoryAndTasks() {
        AuditInstance instance = new AuditInstance();
        instance.setId(9L);
        instance.setFlowDefId(2L);
        instance.setEntityType("PlanReport");
        instance.setEntityId(100L);
        instance.setRequesterId(1L);
        instance.setStartedAt(LocalDateTime.now().minusDays(1));
        instance.setCompletedAt(LocalDateTime.now());
        instance.setStatus(AuditInstance.STATUS_APPROVED);

        AuditStepInstance approved = new AuditStepInstance();
        approved.setId(91L);
        approved.setStepDefId(6L);
        approved.setStepNo(1);
        approved.setStepName("一级审批");
        approved.setStatus(AuditInstance.STEP_STATUS_APPROVED);
        approved.setApproverId(11L);
        approved.setCreatedAt(LocalDateTime.now().minusHours(2));
        approved.setApprovedAt(LocalDateTime.now().minusHours(1));
        instance.addStepInstance(approved);

        User approver = new User();
        approver.setId(11L);
        approver.setRealName("审批人");
        when(userRepository.findById(11L)).thenReturn(Optional.of(approver));

        when(auditInstanceRepository.findById(9L)).thenReturn(Optional.of(instance));

        var detail = newService().getInstanceDetail("9");

        assertEquals(1, detail.getTasks().size());
        assertEquals(3, detail.getHistory().size());
        assertEquals(null, detail.getCurrentTaskId());
        assertEquals("审批人", detail.getTasks().get(0).getAssigneeName());
        assertEquals("PlanReport#100", detail.getHistory().get(0).getTaskName());
        assertEquals("FINISH_APPROVE", detail.getHistory().get(2).getComment());
    }
}
