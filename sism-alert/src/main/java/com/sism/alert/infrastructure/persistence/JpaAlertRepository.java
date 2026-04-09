package com.sism.alert.infrastructure.persistence;

import com.sism.alert.domain.Alert;
import com.sism.alert.domain.enums.AlertStatus;
import com.sism.alert.domain.repository.AlertRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * JpaAlertRepository - Alert 的 JPA 仓储实现
 * 同时继承 JpaRepository 和领域层 AlertRepository 接口
 * 位于 infrastructure.persistence 包下，可被 @EnableJpaRepositories 扫描到
 */
@Repository
public interface JpaAlertRepository extends JpaRepository<Alert, Long>, AlertRepository {

    @Override
    List<Alert> findByStatus(AlertStatus status);

    @Override
    List<Alert> findBySeverity(String severity);

    @Override
    List<Alert> findByIndicatorId(Long indicatorId);

    @Override
    List<Alert> findBySeverityAndStatusNot(String severity, AlertStatus status);

    @Override
    long countByStatus(AlertStatus status);

    @Override
    long countBySeverity(String severity);

    @Override
    long countBySeverityAndStatus(String severity, AlertStatus status);

    @Override
    long countByIndicatorIdIn(Collection<Long> indicatorIds);

    @Override
    long countByIndicatorIdInAndStatus(Collection<Long> indicatorIds, AlertStatus status);

    @Override
    long countByIndicatorIdInAndSeverityAndStatus(Collection<Long> indicatorIds, String severity, AlertStatus status);
}
