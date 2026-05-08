package com.sism.strategy.infrastructure;

import com.sism.organization.domain.OrganizationRepository;
import com.sism.shared.domain.workflow.WorkflowBusinessContextPort;
import com.sism.strategy.domain.plan.Plan;
import com.sism.strategy.domain.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StrategyWorkflowBusinessContextAdapter implements WorkflowBusinessContextPort {

    private static final String PLAN_ENTITY_TYPE = "PLAN";

    private final PlanRepository planRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public Optional<Long> getPlanIdByEntity(String entityType, Long entityId) {
        if (!PLAN_ENTITY_TYPE.equalsIgnoreCase(entityType) || entityId == null) {
            return Optional.empty();
        }
        return Optional.of(entityId);
    }

    @Override
    public Optional<BusinessSummary> getBusinessSummary(String entityType, Long entityId) {
        if (!PLAN_ENTITY_TYPE.equalsIgnoreCase(entityType) || entityId == null) {
            return Optional.empty();
        }
        return planRepository.findById(entityId).map(this::toSummary);
    }

    private BusinessSummary toSummary(Plan plan) {
        Long sourceOrgId = plan.getCreatedByOrgId();
        Long targetOrgId = plan.getTargetOrgId();
        return new BusinessSummary(
                plan.getId(),
                "Plan " + plan.getId(),
                sourceOrgId,
                resolveOrgName(sourceOrgId),
                targetOrgId,
                resolveOrgName(targetOrgId),
                "Plan " + plan.getId()
        );
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
