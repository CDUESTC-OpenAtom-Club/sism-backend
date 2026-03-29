package com.sism.strategy.infrastructure.persistence;

import com.sism.strategy.domain.plan.Plan;
import com.sism.strategy.domain.plan.PlanLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaPlanRepositoryInternal extends JpaRepository<Plan, Long> {
    List<Plan> findByTargetOrgId(Long targetOrgId);

    List<Plan> findByCreatedByOrgId(Long createdByOrgId);

    List<Plan> findByCycleId(Long cycleId);

    List<Plan> findByPlanLevel(PlanLevel planLevel);

    java.util.Optional<Plan> findByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
            Long cycleId,
            PlanLevel planLevel,
            Long createdByOrgId,
            Long targetOrgId
    );

    List<Plan> findByStatusIn(List<String> statuses);

    @Query("""
            SELECT p
            FROM Plan p
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
            FROM Plan p
            WHERE p.isDeleted = false
              AND p.status IN :statuses
              AND (:skipCycleFilter = true OR p.cycleId IN :cycleIds)
            """)
    Page<Plan> findPageByStatus(
            @Param("cycleIds") List<Long> cycleIds,
            @Param("skipCycleFilter") boolean skipCycleFilter,
            @Param("statuses") List<String> statuses,
            Pageable pageable
    );
}
