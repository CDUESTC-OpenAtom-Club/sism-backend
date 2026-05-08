package com.sism.task.infrastructure.persistence;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

@Repository
public class PlanBindingRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<PlanBindingInfo> findByPlanId(Long planId) {
        if (planId == null) {
            return Optional.empty();
        }
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT p.cycle_id, p.target_org_id, p.created_by_org_id, p.plan_level
                FROM public.plan p
                WHERE p.id = :planId
                  AND COALESCE(p.is_deleted, false) = false
                """)
                .setParameter("planId", planId)
                .getResultList();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = rows.get(0);
        return Optional.of(new PlanBindingInfo(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                String.valueOf(row[3]).trim().toUpperCase()
        ));
    }

    public record PlanBindingInfo(Long cycleId, Long targetOrgId, Long createdByOrgId, String planLevel) {}
}
