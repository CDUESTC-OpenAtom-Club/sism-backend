package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.report.PlanStatusSyncGateway;
import com.sism.strategy.domain.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StrategyPlanStatusSyncGateway implements PlanStatusSyncGateway {

    private final PlanRepository planRepository;

    @Override
    public void syncBackToDraft(Long planId) {
        if (planId == null || planId <= 0) {
            return;
        }

        planRepository.findById(planId).ifPresent(plan -> {
            if (plan.isEditable()) {
                return;
            }
            plan.withdraw();
            planRepository.save(plan);
        });
    }
}
