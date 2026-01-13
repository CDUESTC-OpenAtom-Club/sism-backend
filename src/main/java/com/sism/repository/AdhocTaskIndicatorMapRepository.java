package com.sism.repository;

import com.sism.entity.AdhocTaskIndicatorMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for AdhocTaskIndicatorMap entity
 * Provides data access methods for adhoc task indicator mapping
 */
@Repository
public interface AdhocTaskIndicatorMapRepository extends JpaRepository<AdhocTaskIndicatorMap, AdhocTaskIndicatorMap.AdhocTaskIndicatorMapId> {

    /**
     * Find all indicator mappings by adhoc task ID
     */
    List<AdhocTaskIndicatorMap> findByAdhocTask_AdhocTaskId(Long adhocTaskId);

    /**
     * Find all indicator mappings by indicator ID
     */
    List<AdhocTaskIndicatorMap> findByIndicator_IndicatorId(Long indicatorId);

    /**
     * Check if a mapping exists for an adhoc task and indicator
     */
    boolean existsByAdhocTask_AdhocTaskIdAndIndicator_IndicatorId(Long adhocTaskId, Long indicatorId);

    /**
     * Count mappings by adhoc task ID
     */
    long countByAdhocTask_AdhocTaskId(Long adhocTaskId);

    /**
     * Count mappings by indicator ID
     */
    long countByIndicator_IndicatorId(Long indicatorId);

    /**
     * Delete all mappings by adhoc task ID
     */
    void deleteByAdhocTask_AdhocTaskId(Long adhocTaskId);

    /**
     * Find adhoc tasks associated with a specific indicator
     */
    @Query("SELECT atim FROM AdhocTaskIndicatorMap atim WHERE atim.indicator.indicatorId = :indicatorId " +
           "AND atim.adhocTask.status NOT IN ('COMPLETED', 'CANCELED')")
    List<AdhocTaskIndicatorMap> findActiveTasksByIndicator(@Param("indicatorId") Long indicatorId);

    /**
     * Find indicators for multiple adhoc tasks
     */
    @Query("SELECT atim FROM AdhocTaskIndicatorMap atim WHERE atim.adhocTask.adhocTaskId IN :adhocTaskIds")
    List<AdhocTaskIndicatorMap> findByAdhocTaskIds(@Param("adhocTaskIds") List<Long> adhocTaskIds);

    /**
     * Find indicator mappings by adhoc task and indicator owner organization
     */
    @Query("SELECT atim FROM AdhocTaskIndicatorMap atim WHERE atim.adhocTask.adhocTaskId = :adhocTaskId " +
           "AND atim.indicator.ownerOrg.orgId = :ownerOrgId")
    List<AdhocTaskIndicatorMap> findByAdhocTaskAndOwnerOrg(@Param("adhocTaskId") Long adhocTaskId,
                                                            @Param("ownerOrgId") Long ownerOrgId);
}
