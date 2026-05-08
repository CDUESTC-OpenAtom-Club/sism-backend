package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.report.PlanReportRepository;
import com.sism.shared.domain.workflow.WorkflowBusinessContextPort;
import com.sism.organization.domain.OrganizationRepository;
import com.sism.strategy.domain.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExecutionWorkflowBusinessContextAdapter implements WorkflowBusinessContextPort {

    private static final String PLAN_REPORT_ENTITY_TYPE = "PLAN_REPORT";
    private static final String LEGACY_PLAN_REPORT_ENTITY_TYPE = "PlanReport";

    private final PlanReportRepository planReportRepository;
    private final PlanRepository planRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public Optional<Long> getPlanIdByEntity(String entityType, Long entityId) {
        if (entityId == null || entityType == null || entityType.isBlank()) {
            return Optional.empty();
        }
        if (!PLAN_REPORT_ENTITY_TYPE.equalsIgnoreCase(entityType)
                && !LEGACY_PLAN_REPORT_ENTITY_TYPE.equalsIgnoreCase(entityType)) {
            return Optional.empty();
        }
        return planReportRepository.findById(entityId).map(report -> report.getPlanId());
    }

    @Override
    public Optional<BusinessSummary> getBusinessSummary(String entityType, Long entityId) {
        if (entityId == null || entityType == null || entityType.isBlank()) {
            return Optional.empty();
        }
        if (!PLAN_REPORT_ENTITY_TYPE.equalsIgnoreCase(entityType)
                && !LEGACY_PLAN_REPORT_ENTITY_TYPE.equalsIgnoreCase(entityType)) {
            return Optional.empty();
        }
        return planReportRepository.findById(entityId).map(report -> {
            Long sourceOrgId = null;
            String sourceOrgName = null;
            Long targetOrgId = null;
            String targetOrgName = null;
            Long planId = report.getPlanId();
            if (planId != null) {
                planRepository.findById(planId).ifPresent(plan -> {
                });
            }
            var plan = planId == null ? Optional.<com.sism.strategy.domain.plan.Plan>empty() : planRepository.findById(planId);
            if (plan.isPresent()) {
                sourceOrgId = plan.get().getCreatedByOrgId();
                sourceOrgName = resolveOrgName(sourceOrgId);
                targetOrgId = plan.get().getTargetOrgId();
                targetOrgName = resolveOrgName(targetOrgId);
            }
            String reportOrgName = resolveOrgName(report.getReportOrgId());
            String displayName = ((report.getReportMonth() == null ? "" : report.getReportMonth() + " ")
                    + (reportOrgName == null ? "月报" : reportOrgName + "月报")).trim();
            return new BusinessSummary(
                    planId,
                    displayName,
                    sourceOrgId,
                    sourceOrgName,
                    targetOrgId,
                    targetOrgName,
                    displayName
            );
        });
    }

    private String resolveOrgName(Long orgId) {
        if (orgId == null) {
            return null;
        }
        return organizationRepository.findById(orgId)
                .map(org -> org.getName() != null && !org.getName().isBlank() ? org.getName() : "Org#" + orgId)
                .orElse("Org#" + orgId);
    }
}
