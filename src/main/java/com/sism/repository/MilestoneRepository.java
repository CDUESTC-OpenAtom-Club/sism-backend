package com.sism.repository;

import com.sism.entity.Milestone;
import com.sism.enums.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Milestone entity
 * Provides data access methods for milestone management
 */
@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    /**
     * Find all milestones by indicator ID
     */
    List<Milestone> findByIndicator_IndicatorId(Long indicatorId);

    /**
     * Find all milestones by multiple indicator IDs (batch query)
     */
    List<Milestone> findByIndicator_IndicatorIdIn(List<Long> indicatorIds);

    /**
     * Find all milestones by indicator ID ordered by sort order
     */
    List<Milestone> findByIndicator_IndicatorIdOrderBySortOrderAsc(Long indicatorId);

    /**
     * Find all milestones by indicator ID ordered by due date
     */
    List<Milestone> findByIndicator_IndicatorIdOrderByDueDateAsc(Long indicatorId);

    /**
     * Find all milestones by status
     */
    List<Milestone> findByStatus(MilestoneStatus status);

    /**
     * Find all milestones by indicator ID and status
     */
    List<Milestone> findByIndicator_IndicatorIdAndStatus(Long indicatorId, MilestoneStatus status);

    /**
     * Find milestones by due date range
     */
    List<Milestone> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find overdue milestones (due date passed and not completed)
     */
    @Query("SELECT m FROM Milestone m WHERE m.dueDate < :currentDate " +
           "AND m.status NOT IN ('COMPLETED', 'CANCELED')")
    List<Milestone> findOverdueMilestones(@Param("currentDate") LocalDate currentDate);

    /**
     * Find upcoming milestones (due within specified days)
     */
    @Query("SELECT m FROM Milestone m WHERE m.dueDate BETWEEN :startDate AND :endDate " +
           "AND m.status NOT IN ('COMPLETED', 'CANCELED')")
    List<Milestone> findUpcomingMilestones(@Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);

    /**
     * Calculate total weight percentage for an indicator
     */
    // TODO: weight_percent字段已从数据库移除，此方法暂时注释
    // @Query("SELECT COALESCE(SUM(m.weightPercent), 0) FROM Milestone m " +
    //        "WHERE m.indicator.indicatorId = :indicatorId")
    // BigDecimal calculateTotalWeightByIndicator(@Param("indicatorId") Long indicatorId);

    /**
     * Count milestones by indicator ID
     */
    long countByIndicator_IndicatorId(Long indicatorId);

    /**
     * Count milestones by indicator ID and status
     */
    long countByIndicator_IndicatorIdAndStatus(Long indicatorId, MilestoneStatus status);

    /**
     * Find milestones inherited from another milestone
     */
    List<Milestone> findByInheritedFrom_MilestoneId(Long inheritedFromId);

    /**
     * Check if milestone exists by indicator ID and name
     */
    boolean existsByIndicator_IndicatorIdAndMilestoneName(Long indicatorId, String milestoneName);

    /**
     * Find milestones by indicator and due date range with status filter
     */
    @Query("SELECT m FROM Milestone m WHERE m.indicator.indicatorId = :indicatorId " +
           "AND m.dueDate BETWEEN :startDate AND :endDate " +
           "AND m.status = :status")
    List<Milestone> findByIndicatorAndDateRangeAndStatus(@Param("indicatorId") Long indicatorId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate,
                                                          @Param("status") MilestoneStatus status);

    /**
     * Find the earliest unpaired milestone for an indicator
     * A milestone is considered "unpaired" if it has no APPROVED progress report
     */
    @Query("SELECT m FROM Milestone m WHERE m.indicator.indicatorId = :indicatorId " +
           "AND m.status != 'CANCELED' " +
           "AND NOT EXISTS (SELECT r FROM ProgressReport r WHERE r.milestone = m AND r.status = 'APPROVED') " +
           "ORDER BY m.dueDate ASC, m.sortOrder ASC")
    List<Milestone> findUnpairedMilestonesByIndicator(@Param("indicatorId") Long indicatorId);

    /**
     * Find the first (earliest) unpaired milestone for an indicator
     */
    @Query("SELECT m FROM Milestone m WHERE m.indicator.indicatorId = :indicatorId " +
           "AND m.status != 'CANCELED' " +
           "AND NOT EXISTS (SELECT r FROM ProgressReport r WHERE r.milestone = m AND r.status = 'APPROVED') " +
           "ORDER BY m.dueDate ASC, m.sortOrder ASC LIMIT 1")
    Optional<Milestone> findFirstUnpairedMilestone(@Param("indicatorId") Long indicatorId);

    /**
     * Check if a milestone is paired (has an approved report)
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM ProgressReport r WHERE r.milestone.milestoneId = :milestoneId AND r.status = 'APPROVED'")
    boolean isMilestonePaired(@Param("milestoneId") Long milestoneId);
}
