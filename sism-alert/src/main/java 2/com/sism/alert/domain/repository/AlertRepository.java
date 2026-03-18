package com.sism.alert.domain.repository;

import com.sism.alert.domain.Alert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AlertRepository - 预警仓储接口（领域层）
 * 定义领域层所需的仓储方法
 * 实际 JPA 实现位于 infrastructure.persistence.JpaAlertRepository
 */
public interface AlertRepository {

    Optional<Alert> findById(Long id);

    List<Alert> findAll();

    Alert save(Alert alert);

    void delete(Alert alert);

    long count();

    List<Alert> findByStatus(String status);

    List<Alert> findByAlertType(String alertType);

    List<Alert> findBySeverity(String severity);

    List<Alert> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<Alert> findByStatusAndTriggeredAtAfter(String status, LocalDateTime startTime);

    List<Alert> findUnresolvedBySeverity(String severity);

    long countByStatus(String status);

    long countBySeverityAndStatus(String severity, String status);
}
