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
        instance.setStatus(AuditInstance.STATUS_REJECTED);

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

        verify(planApplicationService).markWorkflowPending(100L);
        verify(planApplicationService, org.mockito.Mockito.never()).markWorkflowRejected(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
        verify(planApplicationService, org.mockito.Mockito.never()).markWorkflowWithdrawn(100L);
    }

    @Test
    void shouldKeepPlanPendingWhenInReviewInstanceContainsHistoricalRejectedStep() {
        when(planApplicationServiceProvider.getIfAvailable()).thenReturn(planApplicationService);

        PlanWorkflowSyncService service = new PlanWorkflowSyncService(planApplicationServiceProvider, reportApplicationServiceProvider);
        AuditInstance instance = new AuditInstance();
        instance.setEntityId(101L);
        instance.setEntityType("PLAN");
        instance.setStatus(AuditInstance.STATUS_PENDING);

        AuditStepInstance historicalRejected = new AuditStepInstance();
        historicalRejected.setStatus(AuditInstance.STEP_STATUS_REJECTED);
        historicalRejected.setComment("old reason");
        historicalRejected.setStepNo(2);
        instance.addStepInstance(historicalRejected);

        AuditStepInstance currentPending = new AuditStepInstance();
        currentPending.setStatus(AuditInstance.STEP_STATUS_PENDING);
        currentPending.setStepNo(5);
        instance.addStepInstance(currentPending);

        service.syncAfterWorkflowChanged(instance);

        verify(planApplicationService).markWorkflowPending(101L);
        verify(planApplicationService, never()).markWorkflowRejected(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
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

    @Test
    void shouldSyncWithdrawnPlanReportBackToDraft() {
        when(reportApplicationServiceProvider.getIfAvailable()).thenReturn(reportApplicationService);

        PlanWorkflowSyncService service = new PlanWorkflowSyncService(planApplicationServiceProvider, reportApplicationServiceProvider);
        AuditInstance instance = new AuditInstance();
        instance.setEntityId(201L);
        instance.setEntityType("PLAN_REPORT");
        instance.setRequesterId(267L);
        instance.setStatus(AuditInstance.STATUS_WITHDRAWN);

        AuditStepInstance submitStep = new AuditStepInstance();
        submitStep.setStepNo(1);
        submitStep.setStatus(AuditInstance.STEP_STATUS_APPROVED);
        instance.addStepInstance(submitStep);

        AuditStepInstance withdrawnStep = new AuditStepInstance();
        withdrawnStep.setStepNo(2);
        withdrawnStep.setStatus(AuditInstance.STEP_STATUS_WITHDRAWN);
        withdrawnStep.setComment("提交人撤回");
        instance.addStepInstance(withdrawnStep);

        service.syncAfterWorkflowChanged(instance);

        verify(reportApplicationService).markWorkflowWithdrawn(201L);
        verify(reportApplicationService, never()).markWorkflowRejected(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldKeepPlanReportInReviewWhenRejectedFlowReturnsToSubmitStep() {
        when(reportApplicationServiceProvider.getIfAvailable()).thenReturn(reportApplicationService);

        PlanWorkflowSyncService service = new PlanWorkflowSyncService(planApplicationServiceProvider, reportApplicationServiceProvider);
        AuditInstance instance = new AuditInstance();
        instance.setEntityId(202L);
        instance.setEntityType("PLAN_REPORT");
        instance.setRequesterId(268L);
        instance.setStatus(AuditInstance.STATUS_PENDING);

        AuditStepInstance rejectedStep = new AuditStepInstance();
        rejectedStep.setStepNo(2);
        rejectedStep.setStatus(AuditInstance.STEP_STATUS_REJECTED);
        rejectedStep.setComment("退回修改");
        instance.addStepInstance(rejectedStep);

        AuditStepInstance returnedSubmitStep = new AuditStepInstance();
        returnedSubmitStep.setStepNo(3);
        returnedSubmitStep.setStepName("填报人提交");
        returnedSubmitStep.setStatus(AuditInstance.STEP_STATUS_WITHDRAWN);
        returnedSubmitStep.setComment("驳回后退回填报人重新提交");
        returnedSubmitStep.setApproverId(268L);
        instance.addStepInstance(returnedSubmitStep);

        service.syncAfterWorkflowChanged(instance);

        verify(reportApplicationService).markWorkflowReturnedForResubmission(202L, null);
        verify(reportApplicationService, never()).markWorkflowWithdrawn(202L);
        verify(reportApplicationService, never()).markWorkflowRejected(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
        verify(reportApplicationService, never()).markWorkflowApproved(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldResetPlanReportToDraftWhenCurrentInstanceReturnsToSubmitStep() {
        when(reportApplicationServiceProvider.getIfAvailable()).thenReturn(reportApplicationService);

        PlanWorkflowSyncService service = new PlanWorkflowSyncService(planApplicationServiceProvider, reportApplicationServiceProvider);
        AuditInstance instance = new AuditInstance();
        instance.setId(303L);
        instance.setEntityId(203L);
        instance.setEntityType("PLAN_REPORT");
        instance.setRequesterId(191L);
        instance.setStatus(AuditInstance.STATUS_PENDING);

        AuditStepInstance rejectedStep = new AuditStepInstance();
        rejectedStep.setStepNo(2);
        rejectedStep.setStatus(AuditInstance.STEP_STATUS_REJECTED);
        instance.addStepInstance(rejectedStep);

        AuditStepInstance returnedSubmitStep = new AuditStepInstance();
        returnedSubmitStep.setStepNo(3);
        returnedSubmitStep.setStepName("填报人提交");
        returnedSubmitStep.setStatus(AuditInstance.STEP_STATUS_WITHDRAWN);
        returnedSubmitStep.setApproverId(191L);
        instance.addStepInstance(returnedSubmitStep);

        service.syncAfterWorkflowChanged(instance);

        verify(reportApplicationService).markWorkflowReturnedForResubmission(203L, 303L);
    }

    @Test
    void shouldResetPlanReportToDraftWhenReturnedSubmitStepIsFollowedByWaitingReplayStep() {
        when(reportApplicationServiceProvider.getIfAvailable()).thenReturn(reportApplicationService);

        PlanWorkflowSyncService service = new PlanWorkflowSyncService(planApplicationServiceProvider, reportApplicationServiceProvider);
        AuditInstance instance = new AuditInstance();
        instance.setId(404L);
        instance.setEntityId(204L);
        instance.setEntityType("PLAN_REPORT");
        instance.setRequesterId(191L);
        instance.setStatus(AuditInstance.STATUS_PENDING);

        AuditStepInstance rejectedStep = new AuditStepInstance();
        rejectedStep.setStepNo(2);
        rejectedStep.setStatus(AuditInstance.STEP_STATUS_REJECTED);
        instance.addStepInstance(rejectedStep);

        AuditStepInstance returnedSubmitStep = new AuditStepInstance();
        returnedSubmitStep.setStepNo(3);
        returnedSubmitStep.setStepName("填报人提交");
        returnedSubmitStep.setStatus(AuditInstance.STEP_STATUS_WITHDRAWN);
        returnedSubmitStep.setApproverId(191L);
        instance.addStepInstance(returnedSubmitStep);

        AuditStepInstance waitingReplayStep = new AuditStepInstance();
        waitingReplayStep.setStepNo(4);
        waitingReplayStep.setStepName("职能部门审批人审批");
        waitingReplayStep.setStatus(AuditInstance.STEP_STATUS_WAITING);
        instance.addStepInstance(waitingReplayStep);

        service.syncAfterWorkflowChanged(instance);

        verify(reportApplicationService).markWorkflowReturnedForResubmission(204L, 404L);
    }
}
