package com.sism.repository;

import com.sism.entity.ProgressReport;
import com.sism.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProgressReport entity
 * Provides data access methods for progress report management with pagination support
 */
@Repository
public interface ReportRepository extends JpaRepository<ProgressReport, Long> {

    /**
     * Find all reports by indicator ID
     */
    List<ProgressReport> findByIndicator_IndicatorId(Long indicatorId);

    /**
     * Find all reports by indicator ID with pagination
     */
    Page<ProgressReport> findByIndicator_IndicatorId(Long indicatorId, Pageable pageable);

    /**
     * Find all reports by milestone ID
     */
    List<ProgressReport> findByMilestone_MilestoneId(Long milestoneId);

    /**
     * Find all reports by adhoc task ID
     */
    List<ProgressReport> findByAdhocTask_AdhocTaskId(Long adhocTaskId);

    /**
     * Find all reports by reporter ID
     */
    List<ProgressReport> findByReporter_UserId(Long reporterId);

    /**
     * Find all reports by reporter ID with pagination
     */
    Page<ProgressReport> findByReporter_UserId(Long reporterId, Pageable pageable);

    /**
     * Find all reports by status
     */
    List<ProgressReport> findByStatus(ReportStatus status);

    /**
     * Find all reports by status with pagination
     */
    Page<ProgressReport> findByStatus(ReportStatus status, Pageable pageable);

    /**
     * Find all reports by indicator ID and status
     */
    List<ProgressReport> findByIndicator_IndicatorIdAndStatus(Long indicatorId, ReportStatus status);

    /**
     * Find all reports by milestone ID and status
     */
    List<ProgressReport> findByMilestone_MilestoneIdAndStatus(Long milestoneId, ReportStatus status);

    /**
     * Find the final version report for a milestone
     */
    Optional<ProgressReport> findByMilestone_MilestoneIdAndIsFinalTrue(Long milestoneId);

    /**
     * Find all final version reports by indicator ID
     */
    List<ProgressReport> findByIndicator_IndicatorIdAndIsFinalTrue(Long indicatorId);

    /**
     * Find reports by date range
     */
    List<ProgressReport> findByReportedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find reports by date range with pagination
     */
    Page<ProgressReport> findByReportedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Count reports by indicator ID
     */
    long countByIndicator_IndicatorId(Long indicatorId);

    /**
     * Count reports by indicator ID and status
     */
    long countByIndicator_IndicatorIdAndStatus(Long indicatorId, ReportStatus status);

    /**
     * Count reports by milestone ID
     */
    long countByMilestone_MilestoneId(Long milestoneId);

    /**
     * Find reports by multiple statuses with pagination
     */
    @Query("SELECT r FROM ProgressReport r WHERE r.status IN :statuses")
    Page<ProgressReport> findByStatusIn(@Param("statuses") List<ReportStatus> statuses, Pageable pageable);

    /**
     * Find reports by indicator and status with pagination
     */
    @Query("SELECT r FROM ProgressReport r WHERE r.indicator.indicatorId = :indicatorId " +
           "AND r.status IN :statuses ORDER BY r.reportedAt DESC")
    Page<ProgressReport> findByIndicatorAndStatusIn(@Param("indicatorId") Long indicatorId,
                                                     @Param("statuses") List<ReportStatus> statuses,
                                                     Pageable pageable);

    /**
     * Find reports by reporter's organization
     */
    @Query("SELECT r FROM ProgressReport r WHERE r.reporter.org.orgId = :orgId")
    List<ProgressReport> findByReporterOrg(@Param("orgId") Long orgId);

    /**
     * Find reports by reporter's organization with pagination
     */
    @Query("SELECT r FROM ProgressReport r WHERE r.reporter.org.orgId = :orgId")
    Page<ProgressReport> findByReporterOrg(@Param("orgId") Long orgId, Pageable pageable);

    /**
     * Find latest report for each milestone
     */
    @Query("SELECT r FROM ProgressReport r WHERE r.milestone.milestoneId = :milestoneId " +
           "ORDER BY r.versionNo DESC")
    List<ProgressReport> findLatestByMilestone(@Param("milestoneId") Long milestoneId);

    /**
     * Find approved reports that achieved milestones
     */
    @Query("SELECT r FROM ProgressReport r WHERE r.status = 'APPROVED' " +
           "AND r.achievedMilestone = true AND r.milestone.milestoneId = :milestoneId")
    List<ProgressReport> findApprovedAchievedByMilestone(@Param("milestoneId") Long milestoneId);

    /**
     * Check if a final version report exists for a milestone
     */
    boolean existsByMilestone_MilestoneIdAndIsFinalTrue(Long milestoneId);
}
