package com.sism.workflow.application;

import com.sism.execution.application.ReportApplicationService;
import com.sism.strategy.application.PlanApplicationService;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanWorkflowSyncServiceTest {

    @Mock
    private ObjectProvider<PlanApplicationService> planApplicationServiceProvider;

    @Mock
    private ObjectProvider<ReportApplicationService> reportApplicationServiceProvider;

    @Mock
    private PlanApplicationService planApplicationService;

    @Mock
    private ReportApplicationService reportApplicationService;

    @Test
    void shouldSkipSyncWhenPlanApplicationServiceIsNotAvailable() {
        when(planApplicationServiceProvider.getIfAvailable()).thenReturn(null);

        PlanWorkflowSyncService service = new PlanWorkflowSyncService(planApplicationServiceProvider, reportApplicationServiceProvider);
        AuditInstance instance = new AuditInstance();
        instance.setEntityId(1L);
        instance.setEntityType("PLAN");
        instance.setStatus(AuditInstance.STATUS_APPROVED);

        service.syncAfterWorkflowChanged(instance);

        verify(planApplicationServiceProvider, atLeastOnce()).getIfAvailable();
        verify(planApplicationService, never()).markWorkflowApproved(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldSyncRejectedPlanUsingLastRejectedStepComment() {
        when(planApplicationServiceProvider.getIfAvailable()).thenReturn(planApplicationService);

        PlanWorkflowSyncService service = new PlanWorkflowSyncService(planApplicationServiceProvider, reportApplicationServiceProvider);
        AuditInstance instance = new AuditInstance();
        instance.setEntityId(99L);
        instance.setEntityType("PLAN");
        instance.setStatus(AuditInstance.STATUS_PENDING);

        AuditStepInstance earlierRejected = new AuditStepInstance();
        earlierRejected.setStatus(AuditInstance.STEP_STATUS_REJECTED);
        earlierRejected.setComment("old reason");
        earlierRejected.setStepNo(1);
        instance.addStepInstance(earlierRejected);

        AuditStepInstance latestRejected = new AuditStepInstance();
        latestRejected.setStatus(AuditInstance.STEP_STATUS_REJECTED);
        latestRejected.setComment("latest reason");
        latestRejected.setStepNo(2);
        instance.addStepInstance(latestRejected);

        service.syncAfterWorkflowChanged(instance);

        verify(planApplicationService).markWorkflowRejected(99L, "latest reason");
    }

    @Test
    void shouldIgnoreInReviewPlanWhenSyncingWorkflowState() {
        when(planApplicationServiceProvider.getIfAvailable()).thenReturn(planApplicationService);

        PlanWorkflowSyncService service = new PlanWorkflowSyncService(planApplicationServiceProvider, reportApplicationServiceProvider);
        AuditInstance instance = new AuditInstance();
        instance.setEntityId(100L);
        instance.setEntityType("PLAN");
        instance.setStatus(AuditInstance.STATUS_PENDING);

        service.syncAfterWorkflowChanged(instance);

        verify(planApplicationService, org.mockito.Mockito.never()).markWorkflowWithdrawn(100L);
    }

    @Test
    void shouldSyncApprovedPlanReportToExecutionContext() {
        when(reportApplicationServiceProvider.getIfAvailable()).thenReturn(reportApplicationService);

        PlanWorkflowSyncService service = new PlanWorkflowSyncService(planApplicationServiceProvider, reportApplicationServiceProvider);
        AuditInstance instance = new AuditInstance();
        instance.setEntityId(200L);
        instance.setEntityType("PLAN_REPORT");
        instance.setRequesterId(66L);
        instance.setStatus(AuditInstance.STATUS_APPROVED);

        service.syncAfterWorkflowChanged(instance);

        verify(reportApplicationService).markWorkflowApproved(200L, 66L);
    }
}
