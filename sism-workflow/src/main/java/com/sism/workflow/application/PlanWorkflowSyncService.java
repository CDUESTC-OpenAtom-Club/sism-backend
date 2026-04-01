package com.sism.workflow.application;

import com.sism.execution.application.ReportApplicationService;
import com.sism.strategy.application.PlanApplicationService;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PlanWorkflowSyncService {

    private static final String PLAN_ENTITY_TYPE = "PLAN";
    private static final String PLAN_REPORT_ENTITY_TYPE = "PLAN_REPORT";
    private static final String LEGACY_PLAN_REPORT_ENTITY_TYPE = "PlanReport";

    private final ObjectProvider<PlanApplicationService> planApplicationServiceProvider;
    private final ObjectProvider<ReportApplicationService> reportApplicationServiceProvider;

    public PlanWorkflowSyncService(
            ObjectProvider<PlanApplicationService> planApplicationServiceProvider,
            ObjectProvider<ReportApplicationService> reportApplicationServiceProvider) {
        this.planApplicationServiceProvider = planApplicationServiceProvider;
        this.reportApplicationServiceProvider = reportApplicationServiceProvider;
    }

    public void syncAfterWorkflowChanged(AuditInstance instance) {
        if (instance == null || instance.getEntityId() == null) {
            return;
        }

        if (PLAN_ENTITY_TYPE.equalsIgnoreCase(instance.getEntityType())) {
            syncPlan(instance);
        } else if (isPlanReportEntityType(instance.getEntityType())) {
            syncPlanReport(instance);
        }
    }

    private void syncPlan(AuditInstance instance) {
        withPlanService(planApplicationService -> {
            if (AuditInstance.STATUS_APPROVED.equals(instance.getStatus())) {
                planApplicationService.markWorkflowApproved(instance.getEntityId());
                return;
            }

            if (AuditInstance.STATUS_PENDING.equals(instance.getStatus())) {
                planApplicationService.markWorkflowPending(instance.getEntityId());
                return;
            }

            if (AuditInstance.STATUS_REJECTED.equals(instance.getStatus())) {
                instance.getStepInstances().stream()
                        .filter(step -> AuditInstance.STEP_STATUS_REJECTED.equals(step.getStatus()))
                        .reduce((first, second) -> second)
                        .ifPresent(step -> planApplicationService.markWorkflowRejected(
                                instance.getEntityId(),
                                step.getComment() == null || step.getComment().isBlank() ? "Rejected" : step.getComment()));
            }
        });
    }

    private void syncPlanReport(AuditInstance instance) {
        withReportService(reportService -> {
            if (AuditInstance.STATUS_APPROVED.equals(instance.getStatus())) {
                log.info("Workflow APPROVED for PlanReport#{}, syncing report status", instance.getEntityId());
                reportService.markWorkflowApproved(instance.getEntityId(), instance.getRequesterId());
                return;
            }

            if (AuditInstance.STATUS_WITHDRAWN.equals(instance.getStatus())) {
                log.info("Workflow WITHDRAWN for PlanReport#{}, resetting report status to draft", instance.getEntityId());
                reportService.markWorkflowWithdrawn(instance.getEntityId());
                return;
            }

            if (AuditInstance.STATUS_PENDING.equals(instance.getStatus()) && hasReturnedSubmitStep(instance)) {
                log.info("Workflow RETURNED for PlanReport#{}, resetting report status to draft while keeping instance {}", instance.getEntityId(), instance.getId());
                reportService.markWorkflowReturnedForResubmission(instance.getEntityId(), instance.getId());
                return;
            } else if (AuditInstance.STATUS_REJECTED.equals(instance.getStatus())) {
                String reason = instance.getStepInstances().stream()
                        .filter(step -> AuditInstance.STEP_STATUS_REJECTED.equals(step.getStatus()))
                        .reduce((first, second) -> second)
                        .map(step -> step.getComment() == null || step.getComment().isBlank() ? "审批驳回" : step.getComment())
                        .orElse("审批驳回");
                log.info("Workflow REJECTED for PlanReport#{}, reason: {}", instance.getEntityId(), reason);
                reportService.markWorkflowRejected(instance.getEntityId(), instance.getRequesterId(), reason);
            }
        });
    }

    private boolean isPlanReportEntityType(String entityType) {
        return PLAN_REPORT_ENTITY_TYPE.equalsIgnoreCase(entityType)
                || LEGACY_PLAN_REPORT_ENTITY_TYPE.equalsIgnoreCase(entityType);
    }

    private boolean hasReturnedSubmitStep(AuditInstance instance) {
        if (instance == null || instance.getStepInstances() == null || instance.getStepInstances().isEmpty()) {
            return false;
        }

        return instance.getStepInstances().stream()
                .filter(step -> AuditInstance.STEP_STATUS_WITHDRAWN.equals(step.getStatus()))
                .max(java.util.Comparator
                        .comparing((AuditStepInstance step) -> step.getStepNo() == null ? Integer.MIN_VALUE : step.getStepNo())
                        .thenComparing(step -> step.getCreatedAt() == null ? java.time.LocalDateTime.MIN : step.getCreatedAt()))
                .filter(step -> {
                    if (instance.getRequesterId() != null && instance.getRequesterId().equals(step.getApproverId())) {
                        return true;
                    }
                    String stepName = step.getStepName();
                    return stepName != null && stepName.contains("提交");
                })
                .isPresent();
    }

    private void withPlanService(java.util.function.Consumer<PlanApplicationService> action) {
        PlanApplicationService planApplicationService = planApplicationServiceProvider.getIfAvailable();
        if (planApplicationService == null) {
            log.debug("PlanApplicationService not available, skipping plan workflow sync");
            return;
        }
        action.accept(planApplicationService);
    }

    private void withReportService(java.util.function.Consumer<ReportApplicationService> action) {
        ReportApplicationService reportService = reportApplicationServiceProvider.getIfAvailable();
        if (reportService == null) {
            log.debug("ReportApplicationService not available, skipping report workflow sync");
            return;
        }
        action.accept(reportService);
    }
}
