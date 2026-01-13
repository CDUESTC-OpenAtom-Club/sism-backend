package com.sism.repository;

import com.sism.entity.Indicator;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Indicator entity
 * Provides data access methods for indicator management
 */
@Repository
public interface IndicatorRepository extends JpaRepository<Indicator, Long> {

    /**
     * Find all indicators by task ID
     */
    List<Indicator> findByTask_TaskId(Long taskId);

    /**
     * Find all indicators by parent indicator ID
     */
    List<Indicator> findByParentIndicator_IndicatorId(Long parentIndicatorId);

    /**
     * Find all root indicators (no parent) by task ID
     */
    List<Indicator> findByTask_TaskIdAndParentIndicatorIsNull(Long taskId);

    /**
     * Find all indicators by indicator level
     */
    List<Indicator> findByLevel(IndicatorLevel level);

    /**
     * Find all indicators by status
     */
    List<Indicator> findByStatus(IndicatorStatus status);

    /**
     * Find all indicators by owner organization ID
     */
    List<Indicator> findByOwnerOrg_OrgId(Long ownerOrgId);

    /**
     * Find all indicators by target organization ID
     */
    List<Indicator> findByTargetOrg_OrgId(Long targetOrgId);

    /**
     * Find all indicators by task ID and status
     */
    List<Indicator> findByTask_TaskIdAndStatus(Long taskId, IndicatorStatus status);

    /**
     * Find all indicators by task ID and level
     */
    List<Indicator> findByTask_TaskIdAndLevel(Long taskId, IndicatorLevel level);

    /**
     * Find all indicators by year
     */
    List<Indicator> findByYear(Integer year);

    /**
     * Find all indicators by year and status
     */
    List<Indicator> findByYearAndStatus(Integer year, IndicatorStatus status);

    /**
     * Find indicators by task ID ordered by sort order
     */
    List<Indicator> findByTask_TaskIdOrderBySortOrderAsc(Long taskId);

    /**
     * Count indicators by task ID
     */
    long countByTask_TaskId(Long taskId);

    /**
     * Count indicators by parent indicator ID
     */
    long countByParentIndicator_IndicatorId(Long parentIndicatorId);

    /**
     * Find indicators by organization hierarchy
     * Returns indicators where the target organization matches or is a descendant
     */
    @Query("SELECT DISTINCT i FROM Indicator i " +
           "WHERE i.targetOrg.orgId = :orgId OR i.targetOrg.parentOrg.orgId = :orgId")
    List<Indicator> findByTargetOrgHierarchy(@Param("orgId") Long orgId);

    /**
     * Find indicators issued by a specific organization (owner)
     */
    @Query("SELECT i FROM Indicator i WHERE i.ownerOrg.orgId = :ownerOrgId AND i.status = :status")
    List<Indicator> findByOwnerOrgAndStatus(@Param("ownerOrgId") Long ownerOrgId, 
                                             @Param("status") IndicatorStatus status);

    /**
     * Search indicators by description keyword
     */
    @Query("SELECT i FROM Indicator i WHERE i.indicatorDesc LIKE %:keyword%")
    List<Indicator> searchByDescriptionKeyword(@Param("keyword") String keyword);

    /**
     * Find all child indicators recursively (for hierarchy traversal)
     */
    @Query("SELECT i FROM Indicator i WHERE i.parentIndicator.indicatorId = :parentId " +
           "OR i.parentIndicator.parentIndicator.indicatorId = :parentId")
    List<Indicator> findDescendantIndicators(@Param("parentId") Long parentId);
}
