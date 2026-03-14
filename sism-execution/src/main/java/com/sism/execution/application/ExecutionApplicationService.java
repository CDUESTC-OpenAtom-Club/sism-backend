package com.sism.execution.application;

import com.sism.execution.domain.model.plan.Plan;
import com.sism.execution.domain.model.plan.PlanLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExecutionApplicationService {

    public Plan createPlan(Long cycleId, Long targetOrgId, Long createdByOrgId, PlanLevel planLevel) {
        return Plan.create(cycleId, targetOrgId, createdByOrgId, planLevel);
    }

    public Plan activatePlan(Plan plan) {
        plan.activate();
        return plan;
    }

    public List<Plan> getAllPlans() {
        return new ArrayList<>();
    }

    public Plan getPlanById(Long id) {
        return null;
    }
}
