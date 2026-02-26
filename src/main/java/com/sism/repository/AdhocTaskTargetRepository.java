package com.sism.repository;

import com.sism.entity.AdhocTaskTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for AdhocTaskTarget entity
 * Provides data access methods for adhoc task target organization mapping
 */
@Repository
public interface AdhocTaskTargetRepository extends JpaRepository<AdhocTaskTarget, AdhocTaskTarget.AdhocTaskTargetId> {

    /**
     * Find all targets by adhoc task ID
     */
    List<AdhocTaskTarget> findByAdhocTask_AdhocTaskId(Long adhocTaskId);

    /**
     * Find all targets by target organization ID
     */
    List<AdhocTaskTarget> findByTargetOrg_Id(Long targetOrgId);

    /**
     * Check if a target exists for an adhoc task and organization
     */
    boolean existsByAdhocTask_AdhocTaskIdAndTargetOrg_Id(Long adhocTaskId, Long targetOrgId);

    /**
     * Count targets by adhoc task ID
     */
    long countByAdhocTask_AdhocTaskId(Long adhocTaskId);

    /**
     * Count targets by target organization ID
     */
    long countByTargetOrg_Id(Long targetOrgId);

    /**
     * Delete all targets by adhoc task ID
     */
    void deleteByAdhocTask_AdhocTaskId(Long adhocTaskId);

    /**
     * Find adhoc tasks targeting a specific organization
     */
    @Query("SELECT att FROM AdhocTaskTarget att WHERE att.targetOrg.id = :orgId " +
           "AND att.adhocTask.status NOT IN ('COMPLETED', 'CANCELED')")
    List<AdhocTaskTarget> findActiveTasksByTargetOrg(@Param("orgId") Long orgId);

    /**
     * Find target organizations for multiple adhoc tasks
     */
    @Query("SELECT att FROM AdhocTaskTarget att WHERE att.adhocTask.adhocTaskId IN :adhocTaskIds")
    List<AdhocTaskTarget> findByAdhocTaskIds(@Param("adhocTaskIds") List<Long> adhocTaskIds);
}
