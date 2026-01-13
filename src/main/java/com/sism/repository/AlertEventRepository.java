package com.sism.repository;

import com.sism.entity.AlertEvent;
import com.sism.enums.AlertSeverity;
import com.sism.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for AlertEvent entity
 * Provides data access methods for alert event management with pagination support
 */
@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {

    /**
     * Find all alert events by indicator ID
     */
    List<AlertEvent> findByIndicator_IndicatorId(Long indicatorId);

    /**
     * Find all alert events by indicator ID with pagination
     */
    Page<AlertEvent> findByIndicator_IndicatorId(Long indicatorId, Pageable pageable);

    /**
     * Find all alert events by alert window ID
     */
    List<AlertEvent> findByWindow_WindowId(Long windowId);

    /**
     * Find all alert events by alert rule ID
     */
    List<AlertEvent> findByRule_RuleId(Long ruleId);

    /**
     * Find all alert events by severity
     */
    List<AlertEvent> findBySeverity(AlertSeverity severity);

    /**
     * Find all alert events by severity with pagination
     */
    Page<AlertEvent> findBySeverity(AlertSeverity severity, Pageable pageable);

    /**
     * Find all alert events by status
     */
    List<AlertEvent> findByStatus(AlertStatus status);

    /**
     * Find all alert events by status with pagination
     */
    Page<AlertEvent> findByStatus(AlertStatus status, Pageable pageable);

    /**
     * Find all alert events by severity and status
     */
    List<AlertEvent> findBySeverityAndStatus(AlertSeverity severity, AlertStatus status);

    /**
     * Find all alert events by severity and status with pagination
     */
    Page<AlertEvent> findBySeverityAndStatus(AlertSeverity severity, AlertStatus status, Pageable pageable);

    /**
     * Find all open alert events with pagination
     */
    Page<AlertEvent> findByStatusOrderByCreatedAtDesc(AlertStatus status, Pageable pageable);

    /**
     * Find alert events handled by a specific user
     */
    List<AlertEvent> findByHandledBy_UserId(Long handledById);

    /**
     * Find alert events handled by a specific user with pagination
     */
    Page<AlertEvent> findByHandledBy_UserId(Long handledById, Pageable pageable);

    /**
     * Count alert events by indicator ID
     */
    long countByIndicator_IndicatorId(Long indicatorId);

    /**
     * Count alert events by status
     */
    long countByStatus(AlertStatus status);

    /**
     * Count alert events by severity
     */
    long countBySeverity(AlertSeverity severity);

    /**
     * Count alert events by severity and status
     */
    long countBySeverityAndStatus(AlertSeverity severity, AlertStatus status);

    /**
     * Find alert events by multiple severities with pagination
     */
    @Query("SELECT ae FROM AlertEvent ae WHERE ae.severity IN :severities ORDER BY ae.createdAt DESC")
    Page<AlertEvent> findBySeverityIn(@Param("severities") List<AlertSeverity> severities, Pageable pageable);

    /**
     * Find alert events by multiple statuses with pagination
     */
    @Query("SELECT ae FROM AlertEvent ae WHERE ae.status IN :statuses ORDER BY ae.createdAt DESC")
    Page<AlertEvent> findByStatusIn(@Param("statuses") List<AlertStatus> statuses, Pageable pageable);

    /**
     * Find alert events by indicator's target organization
     */
    @Query("SELECT ae FROM AlertEvent ae WHERE ae.indicator.targetOrg.orgId = :orgId")
    List<AlertEvent> findByTargetOrg(@Param("orgId") Long orgId);

    /**
     * Find alert events by indicator's target organization with pagination
     */
    @Query("SELECT ae FROM AlertEvent ae WHERE ae.indicator.targetOrg.orgId = :orgId " +
           "ORDER BY ae.createdAt DESC")
    Page<AlertEvent> findByTargetOrg(@Param("orgId") Long orgId, Pageable pageable);

    /**
     * Find alert events by window and status
     */
    @Query("SELECT ae FROM AlertEvent ae WHERE ae.window.windowId = :windowId " +
           "AND ae.status = :status ORDER BY ae.severity DESC, ae.createdAt DESC")
    List<AlertEvent> findByWindowAndStatus(@Param("windowId") Long windowId, 
                                           @Param("status") AlertStatus status);

    /**
     * Find critical open alerts
     */
    @Query("SELECT ae FROM AlertEvent ae WHERE ae.severity = 'CRITICAL' " +
           "AND ae.status = 'OPEN' ORDER BY ae.createdAt DESC")
    List<AlertEvent> findCriticalOpenAlerts();

    /**
     * Find critical open alerts with pagination
     */
    @Query("SELECT ae FROM AlertEvent ae WHERE ae.severity = 'CRITICAL' " +
           "AND ae.status = 'OPEN' ORDER BY ae.createdAt DESC")
    Page<AlertEvent> findCriticalOpenAlerts(Pageable pageable);
}
