package com.sism.repository;

import com.sism.entity.ApprovalRecord;
import com.sism.enums.ApprovalAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for ApprovalRecord entity
 * Provides data access methods for approval record management with pagination support
 */
@Repository
public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long> {

    /**
     * Find all approval records by report ID
     */
    List<ApprovalRecord> findByReport_ReportId(Long reportId);

    /**
     * Find all approval records by report ID ordered by action time
     */
    List<ApprovalRecord> findByReport_ReportIdOrderByActedAtDesc(Long reportId);

    /**
     * Find all approval records by approver ID
     */
    List<ApprovalRecord> findByApprover_Id(Long approverId);

    /**
     * Find all approval records by approver ID with pagination
     */
    Page<ApprovalRecord> findByApprover_Id(Long approverId, Pageable pageable);

    /**
     * Find all approval records by approval action
     */
    List<ApprovalRecord> findByAction(ApprovalAction action);

    /**
     * Find all approval records by approval action with pagination
     */
    Page<ApprovalRecord> findByAction(ApprovalAction action, Pageable pageable);

    /**
     * Find approval records by date range
     */
    List<ApprovalRecord> findByActedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find approval records by date range with pagination
     */
    Page<ApprovalRecord> findByActedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Count approval records by report ID
     */
    long countByReport_ReportId(Long reportId);

    /**
     * Count approval records by approver ID
     */
    long countByApprover_Id(Long approverId);

    /**
     * Count approval records by action
     */
    long countByAction(ApprovalAction action);

    /**
     * Find approval records by approver's organization
     */
    @Query("SELECT ar FROM ApprovalRecord ar WHERE ar.approver.org.id = :orgId")
    List<ApprovalRecord> findByApproverOrg(@Param("orgId") Long orgId);

    /**
     * Find approval records by approver's organization with pagination
     */
    @Query("SELECT ar FROM ApprovalRecord ar WHERE ar.approver.org.id = :orgId")
    Page<ApprovalRecord> findByApproverOrg(@Param("orgId") Long orgId, Pageable pageable);

    /**
     * Find approval records by indicator (through report)
     */
    @Query("SELECT ar FROM ApprovalRecord ar WHERE ar.report.indicator.indicatorId = :indicatorId " +
           "ORDER BY ar.actedAt DESC")
    List<ApprovalRecord> findByIndicator(@Param("indicatorId") Long indicatorId);

    /**
     * Find approval records by indicator with pagination
     */
    @Query("SELECT ar FROM ApprovalRecord ar WHERE ar.report.indicator.indicatorId = :indicatorId " +
           "ORDER BY ar.actedAt DESC")
    Page<ApprovalRecord> findByIndicator(@Param("indicatorId") Long indicatorId, Pageable pageable);

    /**
     * Find approval records by action and date range
     */
    @Query("SELECT ar FROM ApprovalRecord ar WHERE ar.action = :action " +
           "AND ar.actedAt BETWEEN :startDate AND :endDate")
    List<ApprovalRecord> findByActionAndDateRange(@Param("action") ApprovalAction action,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Find approval records by multiple actions with pagination
     */
    @Query("SELECT ar FROM ApprovalRecord ar WHERE ar.action IN :actions ORDER BY ar.actedAt DESC")
    Page<ApprovalRecord> findByActionIn(@Param("actions") List<ApprovalAction> actions, Pageable pageable);

    /**
     * Find recent approval records by approver
     */
    @Query("SELECT ar FROM ApprovalRecord ar WHERE ar.approver.id = :approverId " +
           "ORDER BY ar.actedAt DESC")
    List<ApprovalRecord> findRecentByApprover(@Param("approverId") Long approverId, Pageable pageable);
}
