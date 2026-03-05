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
 * Repository interface for Indicator entity - Simplified to match database
 */
@Repository
public interface IndicatorRepository extends JpaRepository<Indicator, Long> {

    /**
     * Find all indicators by task ID (excluding deleted)
     */
    @Query("SELECT i FROM Indicator i WHERE i.taskId = :taskId AND (i.isDeleted = false OR i.isDeleted IS NULL)")
    List<Indicator> findByTaskId(@Param("taskId") Long taskId);

    /**
     * Batch find indicators by multiple task IDs (excluding deleted)
     * Used for optimizing list operations to avoid N+1 query problem
     */
    @Query("SELECT i FROM Indicator i WHERE i.taskId IN :taskIds AND (i.isDeleted = false OR i.isDeleted IS NULL)")
    List<Indicator> findByTaskIdIn(@Param("taskIds") List<Long> taskIds);

    /**
     * Find all indicators by parent indicator ID (excluding deleted)
     */
    @Query("SELECT i FROM Indicator i WHERE i.parentIndicatorId = :parentId AND (i.isDeleted = false OR i.isDeleted IS NULL)")
    List<Indicator> findByParentIndicatorId(@Param("parentId") Long parentIndicatorId);

    /**
     * Find all root indicators (no parent) by task ID (excluding deleted)
     */
    @Query("SELECT i FROM Indicator i WHERE i.taskId = :taskId AND i.parentIndicatorId IS NULL AND (i.isDeleted = false OR i.isDeleted IS NULL)")
    List<Indicator> findByTaskIdAndParentIndicatorIdIsNull(@Param("taskId") Long taskId);

    /**
     * Find indicators by task ID ordered by sort order (excluding deleted)
     */
    @Query("SELECT i FROM Indicator i WHERE i.taskId = :taskId AND (i.isDeleted = false OR i.isDeleted IS NULL) ORDER BY i.sortOrder ASC")
    List<Indicator> findByTaskIdOrderBySortOrderAsc(@Param("taskId") Long taskId);

    /**
     * Count indicators by task ID (excluding deleted)
     */
    @Query("SELECT COUNT(i) FROM Indicator i WHERE i.taskId = :taskId AND (i.isDeleted = false OR i.isDeleted IS NULL)")
    long countByTaskId(@Param("taskId") Long taskId);

    /**
     * Count indicators by parent indicator ID (excluding deleted)
     */
    List<Indicator> findByOwnerOrg_Id(Long ownerOrgId);

    /**
     * Find all indicators by target organization ID
     */
    List<Indicator> findByTargetOrg_Id(Long targetOrgId);

    /**
     * Find all indicators by task ID and status
     */
    List<Indicator> findByTaskIdAndStatus(Long taskId, IndicatorStatus status);

    /**
     * Find all indicators by task ID and level
     */
    List<Indicator> findByTaskIdAndLevel(Long taskId, IndicatorLevel level);

    /**
     * Find all indicators by year
     */
    List<Indicator> findByYear(Integer year);

    /**
     * Find all indicators by year and status
     */
    List<Indicator> findByYearAndStatus(Integer year, IndicatorStatus status);

    /**
     * Count indicators by parent indicator ID
     */
    @Query("SELECT COUNT(i) FROM Indicator i WHERE i.parentIndicatorId = :parentIndicatorId")
    long countByParentIndicatorId(@Param("parentIndicatorId") Long parentIndicatorId);

    /**
     * Find indicators by organization hierarchy
     * Returns indicators where the target organization matches or is a descendant
     */
    @Query("SELECT DISTINCT i FROM Indicator i " +
           "WHERE i.targetOrg.id = :orgId")
    List<Indicator> findByTargetOrgHierarchy(@Param("orgId") Long orgId);

    /**
     * Find indicators issued by a specific organization (owner)
     */
    @Query("SELECT i FROM Indicator i WHERE i.ownerOrg.id = :ownerOrgId AND i.status = :status")
    List<Indicator> findByOwnerOrgAndStatus(@Param("ownerOrgId") Long ownerOrgId, 
                                             @Param("status") IndicatorStatus status);

    /**
     * Search indicators by description keyword
     */
    @Query("SELECT i FROM Indicator i WHERE i.indicatorDesc LIKE %:keyword% AND i.isDeleted = false")
    List<Indicator> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Find all child indicators recursively (for hierarchy traversal)
     */
    @Query("SELECT i FROM Indicator i WHERE i.parentIndicatorId = :parentId " +
           "OR i.parentIndicatorId IN (SELECT i2.indicatorId FROM Indicator i2 WHERE i2.parentIndicatorId = :parentId)")
    List<Indicator> findDescendantIndicators(@Param("parentId") Long parentId);

    /**
     * Find indicators by parent indicator ID
     */
    @Query("SELECT i FROM Indicator i WHERE i.parentIndicatorId = :parentIndicatorId")
    List<Indicator> findByParentIndicatorIdDirect(@Param("parentIndicatorId") Long parentIndicatorId);

    /**
     * Find indicators by type1 and status
     */
    List<Indicator> findByType1AndStatus(String type1, IndicatorStatus status);

    /**
     * Find indicators by type2 and status
     */
    List<Indicator> findByType2AndStatus(String type2, IndicatorStatus status);

    /**
     * Find indicators by isQualitative and status
     */
    List<Indicator> findByIsQualitativeAndStatus(Boolean isQualitative, IndicatorStatus status);

    /**
     * Find indicators by status
     */
    List<Indicator> findByStatus(IndicatorStatus status);

    /**
     * Find indicators by type1, type2 and status
     */
    List<Indicator> findByType1AndType2AndStatus(String type1, String type2, IndicatorStatus status);
    
    /**
     * Find all indicators with eagerly loaded organization relationships
     * This avoids N+1 query problem when accessing ownerOrg and targetOrg
     */
    @Query("SELECT DISTINCT i FROM Indicator i " +
           "LEFT JOIN FETCH i.ownerOrg " +
           "LEFT JOIN FETCH i.targetOrg " +
           "WHERE i.isDeleted = false OR i.isDeleted IS NULL")
    List<Indicator> findAllWithOrganizations();
}
