package com.sism.strategy.domain.repository;

import com.sism.strategy.domain.plan.Plan;
import com.sism.strategy.domain.plan.PlanLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Strategy-owned repository for the planning aggregate.
 */
public interface PlanRepository {

    Optional<Plan> findById(Long id);

    List<Plan> findAll();

    Page<Plan> findPage(List<Long> cycleIds, List<String> statuses, Pageable pageable);

    List<Plan> findByTargetOrgId(Long targetOrgId);

    List<Plan> findByCreatedByOrgId(Long createdByOrgId);

    List<Plan> findByCycleId(Long cycleId);

    List<Plan> findByPlanLevel(PlanLevel planLevel);

    List<Plan> findByStatuses(List<String> statuses);

    Plan save(Plan plan);

    void delete(Plan plan);

    boolean existsById(Long id);
}
