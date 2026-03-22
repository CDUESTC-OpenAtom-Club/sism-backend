package com.sism.workflow.application.query;

import com.sism.workflow.application.definition.WorkflowDefinitionQueryService;
import com.sism.workflow.application.support.ApproverResolver;
import com.sism.iam.domain.Role;
import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.domain.repository.PlanReportRepository;
import com.sism.strategy.domain.plan.Plan;
import com.sism.strategy.domain.repository.PlanRepository;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.query.repository.WorkflowQueryRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import com.sism.workflow.interfaces.dto.PageResult;
import com.sism.workflow.interfaces.dto.WorkflowInstanceDetailResponse;
import com.sism.workflow.interfaces.dto.WorkflowInstanceResponse;
import com.sism.workflow.interfaces.dto.WorkflowHistoryCardResponse;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanReportRepository planReportRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    private WorkflowReadModelService newService() {
        WorkflowReadModelMapper mapper = new WorkflowReadModelMapper(new ApproverResolver(userRepository));
        return new WorkflowReadModelService(
                workflowDefinitionQueryService,
                auditInstanceRepository,
                workflowQueryRepository,
                mapper,
                new ApproverResolver(userRepository),
                planRepository,
                planReportRepository,
                organizationRepository,
                userRepository
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
        instance.setEntityType("PLAN_REPORT");
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
        stepDef.setRoleId(2L);
        flowDef.setSteps(List.of(stepDef));

        User approver = new User();
        approver.setId(101L);
        approver.setRealName("审批人A");
        approver.setIsActive(true);
        approver.setOrgId(44L);
        Role role = new Role();
        role.setId(2L);
        approver.setRoles(java.util.Set.of(role));
        when(userRepository.findById(101L)).thenReturn(Optional.of(approver));
        when(organizationRepository.findById(44L)).thenReturn(Optional.of(namedOrg(44L, "教务处")));
        PlanReport report = PlanReport.createDraft("2026-03", 44L, ReportOrgType.FUNC_DEPT, 501L);
        report.setId(88L);
        when(planReportRepository.findById(88L)).thenReturn(Optional.of(report));
        when(userRepository.findRoleIdsByUserId(101L)).thenReturn(List.of(2L));
        when(auditInstanceRepository.findByStatus(AuditInstance.STATUS_PENDING)).thenReturn(List.of(instance));
        when(workflowDefinitionQueryService.getAuditFlowDefById(2L)).thenReturn(flowDef);

        PageResult<WorkflowTaskResponse> result = newService().getMyPendingTasks(101L, 1);

        assertEquals(1, result.getTotal());
        assertEquals("88", result.getItems().get(0).getTaskId());
        assertEquals("step_5", result.getItems().get(0).getTaskKey());
        assertEquals("审批人A", result.getItems().get(0).getAssigneeName());
        assertEquals("PLAN_REPORT", result.getItems().get(0).getEntityType());
    }

    @Test
    void getMyPendingTasks_shouldFallbackTaskNameToEntityLabelWhenStepNameMissing() {
        AuditInstance instance = new AuditInstance();
        instance.setId(18L);
        instance.setFlowDefId(1L);
        instance.setEntityType("PLAN_REPORT");
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
        stepDef.setRoleId(3L);
        flowDef.setSteps(List.of(stepDef));

        User approver = new User();
        approver.setId(201L);
        approver.setUsername("approver201");
        approver.setIsActive(true);
        approver.setOrgId(35L);
        Role role = new Role();
        role.setId(3L);
        approver.setRoles(java.util.Set.of(role));
        when(userRepository.findById(201L)).thenReturn(Optional.of(approver));
        when(userRepository.findRoleIdsByUserId(201L)).thenReturn(List.of(3L));
        PlanReport report = PlanReport.createDraft("2026-03", 35L, ReportOrgType.FUNC_DEPT, 999L);
        report.setId(123L);
        when(planReportRepository.findById(123L)).thenReturn(Optional.of(report));
        when(organizationRepository.findById(35L)).thenReturn(Optional.of(namedOrg(35L, "战略发展部")));
        when(auditInstanceRepository.findByStatus(AuditInstance.STATUS_PENDING)).thenReturn(List.of(instance));
        when(workflowDefinitionQueryService.getAuditFlowDefById(1L)).thenReturn(flowDef);

        PageResult<WorkflowTaskResponse> result = newService().getMyPendingTasks(201L, 1);

        assertEquals(1, result.getTotal());
        assertEquals("PLAN_REPORT", result.getItems().get(0).getEntityType());
        assertEquals("Unknown", result.getItems().get(0).getAssigneeName());
    }

    @Test
    void getInstanceDetail_shouldBuildHistoryAndTasks() {
        AuditInstance instance = new AuditInstance();
        instance.setId(9L);
        instance.setFlowDefId(2L);
        instance.setEntityType("PLAN_REPORT");
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(namedUser(1L, "发起人")));
        PlanReport report = PlanReport.createDraft("2026-03", 200L, ReportOrgType.FUNC_DEPT, 300L);
        report.setId(100L);
        when(planReportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(organizationRepository.findById(200L)).thenReturn(Optional.of(namedOrg(200L, "教务处")));

        when(auditInstanceRepository.findById(9L)).thenReturn(Optional.of(instance));

        var detail = newService().getInstanceDetail("9");

        assertEquals(1, detail.getTasks().size());
        assertEquals(3, detail.getHistory().size());
        assertEquals(null, detail.getCurrentTaskId());
        assertEquals("审批人", detail.getTasks().get(0).getAssigneeName());
        assertEquals("PLAN_REPORT#100", detail.getHistory().get(0).getTaskName());
        assertEquals("FINISH_APPROVE", detail.getHistory().get(2).getComment());
    }

    @Test
    void getInstanceDetailByBusiness_shouldPreferRejectedOverWithdrawnWhenNoNewerVisibleInstanceExists() {
        AuditInstance approved = new AuditInstance();
        approved.setId(41L);
        approved.setFlowDefId(1L);
        approved.setEntityType("PLAN");
        approved.setEntityId(7071L);
        approved.setRequesterId(188L);
        approved.setRequesterOrgId(35L);
        approved.setStatus(AuditInstance.STATUS_APPROVED);
        approved.setStartedAt(LocalDateTime.of(2026, 3, 22, 8, 8));
        approved.setCompletedAt(LocalDateTime.of(2026, 3, 22, 8, 9));

        AuditInstance withdrawn = new AuditInstance();
        withdrawn.setId(42L);
        withdrawn.setFlowDefId(1L);
        withdrawn.setEntityType("PLAN");
        withdrawn.setEntityId(7071L);
        withdrawn.setRequesterId(188L);
        withdrawn.setRequesterOrgId(35L);
        withdrawn.setStatus(AuditInstance.STATUS_WITHDRAWN);
        withdrawn.setStartedAt(LocalDateTime.of(2026, 3, 22, 8, 10));

        Plan plan = new Plan();
        plan.setId(7071L);
        plan.setCreatedByOrgId(35L);
        plan.setTargetOrgId(44L);

        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setId(1L);
        flowDef.setFlowCode("PLAN_DISPATCH_STRATEGY");
        flowDef.setFlowName("Plan下发审批（战略发展部）");
        flowDef.setSteps(List.of());

        when(auditInstanceRepository.findByBusinessTypeAndBusinessId("PLAN", 7071L))
                .thenReturn(List.of(approved, withdrawn));
        when(planRepository.findById(7071L)).thenReturn(Optional.of(plan));
        when(organizationRepository.findById(35L)).thenReturn(Optional.of(namedOrg(35L, "战略发展部")));
        when(organizationRepository.findById(44L)).thenReturn(Optional.of(namedOrg(44L, "教务处")));
        when(userRepository.findById(188L)).thenReturn(Optional.of(namedUser(188L, "战略部管理员")));
        when(workflowDefinitionQueryService.getAuditFlowDefById(1L)).thenReturn(flowDef);

        WorkflowInstanceDetailResponse detail = newService().getInstanceDetailByBusiness("PLAN", 7071L);

        assertNotNull(detail);
        assertEquals("41", detail.getInstanceId());
        assertEquals("PLAN_DISPATCH_STRATEGY", detail.getFlowCode());
        assertEquals("战略发展部", detail.getSourceOrgName());
        assertEquals("教务处", detail.getTargetOrgName());
    }

    @Test
    void getMyApprovedInstances_shouldReturnEnrichedSummaries() {
        AuditInstance instance = new AuditInstance();
        instance.setId(13L);
        instance.setFlowDefId(3L);
        instance.setEntityType("PLAN");
        instance.setEntityId(7073L);
        instance.setRequesterId(223L);
        instance.setRequesterOrgId(44L);
        instance.setStatus(AuditInstance.STATUS_APPROVED);
        instance.setStartedAt(LocalDateTime.of(2026, 3, 22, 8, 12));
        instance.setCompletedAt(LocalDateTime.of(2026, 3, 22, 8, 14));

        Plan plan = new Plan();
        plan.setId(7073L);
        plan.setCreatedByOrgId(44L);
        plan.setTargetOrgId(44L);

        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setId(3L);
        flowDef.setFlowCode("PLAN_APPROVAL_FUNCDEPT");
        flowDef.setFlowName("Plan审批流程（职能部门）");
        flowDef.setSteps(List.of());

        when(workflowQueryRepository.findApprovedAuditInstancesByUserId(124L)).thenReturn(List.of(instance));
        when(planRepository.findById(7073L)).thenReturn(Optional.of(plan));
        when(organizationRepository.findById(44L)).thenReturn(Optional.of(namedOrg(44L, "教务处")));
        when(userRepository.findById(223L)).thenReturn(Optional.of(namedUser(223L, "教务处填报人")));
        when(workflowDefinitionQueryService.getAuditFlowDefById(3L)).thenReturn(flowDef);

        PageResult<WorkflowInstanceResponse> page = newService().getMyApprovedInstances(124L, 1, 10);

        assertEquals(1, page.getTotal());
        assertEquals("13", page.getItems().get(0).getInstanceId());
        assertEquals("PLAN_APPROVAL_FUNCDEPT", page.getItems().get(0).getFlowCode());
        assertEquals("教务处", page.getItems().get(0).getSourceOrgName());
        assertEquals("教务处", page.getItems().get(0).getTargetOrgName());
        assertEquals(7073L, page.getItems().get(0).getEntityId());
    }

    @Test
    void listInstanceHistoryByBusiness_shouldOnlyIncludeTerminalApprovedInstances() {
        AuditFlowDef flowDef = new AuditFlowDef();
        AuditStepDef terminalStep = new AuditStepDef();
        terminalStep.setId(10L);
        terminalStep.setIsTerminal(true);
        flowDef.setSteps(List.of(terminalStep));

        AuditInstance approved = new AuditInstance();
        approved.setId(300L);
        approved.setFlowDefId(3L);
        approved.setEntityType("PLAN");
        approved.setEntityId(77L);
        approved.setStatus(AuditInstance.STATUS_APPROVED);
        approved.setStartedAt(LocalDateTime.of(2026, 3, 20, 10, 0));
        approved.setCompletedAt(LocalDateTime.of(2026, 3, 20, 12, 0));
        AuditStepInstance approvedTerminal = new AuditStepInstance();
        approvedTerminal.setId(301L);
        approvedTerminal.setStepDefId(10L);
        approvedTerminal.setStepNo(4);
        approvedTerminal.setStatus(AuditInstance.STEP_STATUS_APPROVED);
        approved.addStepInstance(approvedTerminal);

        AuditInstance rejected = new AuditInstance();
        rejected.setId(302L);
        rejected.setFlowDefId(3L);
        rejected.setEntityType("PLAN");
        rejected.setEntityId(77L);
        rejected.setStatus(AuditInstance.STATUS_REJECTED);
        rejected.setStartedAt(LocalDateTime.of(2026, 3, 21, 10, 0));
        AuditStepInstance rejectedTerminal = new AuditStepInstance();
        rejectedTerminal.setId(303L);
        rejectedTerminal.setStepDefId(10L);
        rejectedTerminal.setStepNo(4);
        rejectedTerminal.setStatus(AuditInstance.STEP_STATUS_REJECTED);
        rejected.addStepInstance(rejectedTerminal);

        Plan plan = new Plan();
        plan.setId(77L);
        plan.setCreatedByOrgId(35L);
        plan.setTargetOrgId(44L);

        when(auditInstanceRepository.findByBusinessTypeAndBusinessId("PLAN", 77L))
                .thenReturn(List.of(approved, rejected));
        when(workflowDefinitionQueryService.getAuditFlowDefById(3L)).thenReturn(flowDef);
        when(planRepository.findById(77L)).thenReturn(Optional.of(plan));
        when(organizationRepository.findById(35L)).thenReturn(Optional.of(namedOrg(35L, "战略发展部")));
        when(organizationRepository.findById(44L)).thenReturn(Optional.of(namedOrg(44L, "教务处")));

        List<WorkflowHistoryCardResponse> result = newService().listInstanceHistoryByBusiness("PLAN", 77L);

        assertEquals(1, result.size());
        assertEquals("300", result.get(0).getInstanceId());
        assertEquals(1, result.get(0).getRoundNo());
    }

    @Test
    void getInstanceDetailByBusiness_shouldReturnLatestNonWithdrawnInstance() {
        AuditInstance rejected = new AuditInstance();
        rejected.setId(401L);
        rejected.setFlowDefId(3L);
        rejected.setEntityType("PLAN");
        rejected.setEntityId(88L);
        rejected.setStatus(AuditInstance.STATUS_REJECTED);
        rejected.setStartedAt(LocalDateTime.of(2026, 3, 20, 10, 0));

        AuditInstance withdrawn = new AuditInstance();
        withdrawn.setId(402L);
        withdrawn.setEntityType("PLAN");
        withdrawn.setEntityId(88L);
        withdrawn.setStatus(AuditInstance.STATUS_WITHDRAWN);
        withdrawn.setStartedAt(LocalDateTime.of(2026, 3, 21, 10, 0));

        when(auditInstanceRepository.findByBusinessTypeAndBusinessId("PLAN", 88L))
                .thenReturn(List.of(rejected, withdrawn));
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setId(3L);
        flowDef.setFlowCode("PLAN_APPROVAL_FUNCDEPT");
        flowDef.setFlowName("Plan审批流程（职能部门）");
        when(workflowDefinitionQueryService.getAuditFlowDefById(3L)).thenReturn(flowDef);

        Plan plan = new Plan();
        plan.setId(88L);
        plan.setCreatedByOrgId(35L);
        plan.setTargetOrgId(44L);
        when(planRepository.findById(88L)).thenReturn(Optional.of(plan));
        when(organizationRepository.findById(35L)).thenReturn(Optional.of(namedOrg(35L, "战略发展部")));
        when(organizationRepository.findById(44L)).thenReturn(Optional.of(namedOrg(44L, "教务处")));

        var detail = newService().getInstanceDetailByBusiness("PLAN", 88L);

        assertNotNull(detail);
        assertEquals("401", detail.getInstanceId());
        assertEquals("REJECTED", detail.getStatus());
        assertEquals("PLAN_APPROVAL_FUNCDEPT", detail.getFlowCode());
    }

    private SysOrg namedOrg(Long id, String name) {
        SysOrg org = new SysOrg();
        org.setId(id);
        org.setName(name);
        return org;
    }

    private User namedUser(Long id, String realName) {
        User user = new User();
        user.setId(id);
        user.setRealName(realName);
        return user;
    }
}
