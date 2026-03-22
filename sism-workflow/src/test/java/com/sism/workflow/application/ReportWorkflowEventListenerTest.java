package com.sism.workflow.application;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.domain.model.report.event.PlanReportApprovedEvent;
import com.sism.execution.domain.model.report.event.PlanReportRejectedEvent;
import com.sism.execution.domain.model.report.event.PlanReportSubmittedEvent;
import com.sism.execution.domain.repository.PlanReportRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import com.sism.workflow.interfaces.dto.StartWorkflowRequest;
import com.sism.workflow.interfaces.dto.WorkflowInstanceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportWorkflowEventListenerTest {

    @Mock
    private BusinessWorkflowApplicationService businessWorkflowApplicationService;

    @Mock
    private PlanReportRepository planReportRepository;

    @Mock
    private AuditInstanceRepository auditInstanceRepository;

    @InjectMocks
    private ReportWorkflowEventListener listener;

    @Test
    void handlePlanReportSubmitted_shouldUseSubmitterIdFromEvent() {
        PlanReport report = PlanReport.createDraft("2026-03", 200L, ReportOrgType.FUNC_DEPT, 300L);
        report.setId(100L);

        when(planReportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(businessWorkflowApplicationService.startWorkflow(any(), any(), any()))
                .thenReturn(WorkflowInstanceResponse.builder().instanceId("88").build());

        listener.handlePlanReportSubmitted(new PlanReportSubmittedEvent(100L, "2026-03", 200L, 66L));

        ArgumentCaptor<StartWorkflowRequest> requestCaptor = ArgumentCaptor.forClass(StartWorkflowRequest.class);
        verify(businessWorkflowApplicationService).startWorkflow(requestCaptor.capture(), org.mockito.ArgumentMatchers.eq(66L), org.mockito.ArgumentMatchers.eq(200L));
        assertEquals("PLAN_APPROVAL_FUNCDEPT", requestCaptor.getValue().getWorkflowCode());
        assertEquals(100L, requestCaptor.getValue().getBusinessEntityId());
        assertEquals("PLAN_REPORT", requestCaptor.getValue().getBusinessEntityType());
        assertEquals(66L, requestCaptor.getValue().getVariables().get("submitterId"));
        assertEquals(88L, report.getAuditInstanceId());
        verify(planReportRepository).save(report);
    }

    @Test
    void handlePlanReportApproved_shouldSyncActiveWorkflowInstance() {
        PlanReport report = PlanReport.createDraft("2026-03", 200L, ReportOrgType.FUNC_DEPT, 300L);
        report.setId(100L);
        report.setStatus(PlanReport.STATUS_APPROVED);

        AuditInstance instance = new AuditInstance();
        instance.setStatus(AuditInstance.STATUS_PENDING);
        AuditStepInstance step = new AuditStepInstance();
        step.setStatus(AuditInstance.STEP_STATUS_PENDING);
        instance.addStepInstance(step);

        when(planReportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(auditInstanceRepository.findByBusinessTypeAndBusinessId("PLAN_REPORT", 100L))
                .thenReturn(List.of());
        when(auditInstanceRepository.findByBusinessTypeAndBusinessId("PlanReport", 100L))
                .thenReturn(List.of(instance));

        listener.handlePlanReportApproved(new PlanReportApprovedEvent(100L, "2026-03", 200L, 99L));

        assertEquals(AuditInstance.STATUS_APPROVED, instance.getStatus());
        assertEquals(AuditInstance.STEP_STATUS_APPROVED, instance.getStepInstances().get(0).getStatus());
        assertEquals(99L, instance.getStepInstances().get(0).getApproverId());
        verify(auditInstanceRepository).save(instance);
    }

    @Test
    void handlePlanReportRejected_shouldSyncActiveWorkflowInstance() {
        PlanReport report = PlanReport.createDraft("2026-03", 200L, ReportOrgType.FUNC_DEPT, 300L);
        report.setId(100L);
        report.setStatus(PlanReport.STATUS_REJECTED);

        AuditInstance instance = new AuditInstance();
        instance.setStatus(AuditInstance.STATUS_PENDING);
        AuditStepInstance step = new AuditStepInstance();
        step.setStatus(AuditInstance.STEP_STATUS_PENDING);
        instance.addStepInstance(step);

        when(planReportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(auditInstanceRepository.findByBusinessTypeAndBusinessId("PLAN_REPORT", 100L))
                .thenReturn(List.of());
        when(auditInstanceRepository.findByBusinessTypeAndBusinessId("PlanReport", 100L))
                .thenReturn(List.of(instance));

        listener.handlePlanReportRejected(new PlanReportRejectedEvent(100L, "2026-03", 200L, 77L, "need changes"));

        assertEquals(AuditInstance.STATUS_REJECTED, instance.getStatus());
        assertEquals(AuditInstance.STEP_STATUS_REJECTED, instance.getStepInstances().get(0).getStatus());
        assertTrue(instance.getStepInstances().get(0).getComment().contains("need changes"));
        verify(auditInstanceRepository).save(instance);
    }
}
