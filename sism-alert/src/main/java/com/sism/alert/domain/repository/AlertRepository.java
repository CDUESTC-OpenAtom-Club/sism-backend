package com.sism.alert.domain.repository;

import com.sism.alert.domain.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AlertRepository - 预警仓储接口
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByStatus(String status);

    List<Alert> findByAlertType(String alertType);

    List<Alert> findBySeverity(String severity);

    List<Alert> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @Query("SELECT a FROM Alert a WHERE a.status = :status AND a.triggeredAt >= :startTime")
    List<Alert> findByStatusAndTriggeredAtAfter(String status, LocalDateTime startTime);

    @Query("SELECT a FROM Alert a WHERE a.severity = :severity AND a.status != 'RESOLVED'")
    List<Alert> findUnresolvedBySeverity(String severity);

    long countByStatus(String status);

    long countBySeverityAndStatus(String severity, String status);
}
