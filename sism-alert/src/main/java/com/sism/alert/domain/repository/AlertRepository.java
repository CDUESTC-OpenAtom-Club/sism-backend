package com.sism.alert.domain.repository;

import com.sism.alert.domain.Alert;
import com.sism.alert.domain.enums.AlertSeverity;
import com.sism.alert.domain.enums.AlertStatus;

import java.util.Collection;
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

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findBySeverity(String severity);

    List<Alert> findByIndicatorId(Long indicatorId);

    List<Alert> findBySeverityAndStatusNot(String severity, AlertStatus status);

    default List<Alert> findUnresolvedBySeverity(String severity) {
        String normalizedSeverity = AlertSeverity.normalize(severity);
        if (normalizedSeverity == null) {
            return List.of();
        }
        return findBySeverityAndStatusNot(normalizedSeverity, AlertStatus.RESOLVED);
    }

    long countByStatus(AlertStatus status);

    long countBySeverity(String severity);

    long countBySeverityAndStatus(String severity, AlertStatus status);

    long countByIndicatorIdIn(Collection<Long> indicatorIds);

    long countByIndicatorIdInAndStatus(Collection<Long> indicatorIds, AlertStatus status);

    long countByIndicatorIdInAndSeverityAndStatus(Collection<Long> indicatorIds, String severity, AlertStatus status);
}
