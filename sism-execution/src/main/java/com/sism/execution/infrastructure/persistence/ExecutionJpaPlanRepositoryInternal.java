package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.model.plan.Plan;
import com.sism.execution.domain.model.plan.PlanLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ExecutionJpaPlanRepositoryInternal extends JpaRepository<Plan, Long> {

    List<Plan> findByTargetOrgId(Long targetOrgId);

    List<Plan> findByCreatedByOrgId(Long createdByOrgId);

    List<Plan> findByCycleId(Long cycleId);

    List<Plan> findByPlanLevel(PlanLevel planLevel);

    List<Plan> findByStatus(String status);

    @Query("""
            SELECT p
            FROM ExecutionPlan p
            WHERE p.isDeleted = false
              AND (:skipCycleFilter = true OR p.cycleId IN :cycleIds)
            """)
    Page<Plan> findPage(
            @Param("cycleIds") List<Long> cycleIds,
            @Param("skipCycleFilter") boolean skipCycleFilter,
            Pageable pageable
    );

    @Query("""
            SELECT p
            FROM ExecutionPlan p
            WHERE p.isDeleted = false
              AND p.status = :status
              AND (:skipCycleFilter = true OR p.cycleId IN :cycleIds)
            """)
    Page<Plan> findPageByStatus(
            @Param("cycleIds") List<Long> cycleIds,
            @Param("skipCycleFilter") boolean skipCycleFilter,
            @Param("status") String status,
            Pageable pageable
    );
}
