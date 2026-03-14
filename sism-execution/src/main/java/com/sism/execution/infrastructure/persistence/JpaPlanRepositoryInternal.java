package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.model.plan.Plan;
import com.sism.execution.domain.model.plan.PlanLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaPlanRepositoryInternal extends JpaRepository<Plan, Long> {

    List<Plan> findByTargetOrgId(Long targetOrgId);

    List<Plan> findByCreatedByOrgId(Long createdByOrgId);

    List<Plan> findByCycleId(Long cycleId);

    List<Plan> findByPlanLevel(PlanLevel planLevel);

    List<Plan> findByStatus(String status);
}
