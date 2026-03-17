package com.sism.alert.infrastructure.persistence;

import com.sism.alert.domain.Alert;
import com.sism.alert.domain.repository.AlertRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JpaAlertRepository - Alert 的 JPA 仓储实现
 * 同时继承 JpaRepository 和领域层 AlertRepository 接口
 * 位于 infrastructure.persistence 包下，可被 @EnableJpaRepositories 扫描到
 */
@Repository
public interface JpaAlertRepository extends JpaRepository<Alert, Long>, AlertRepository {

    @Override
    List<Alert> findByStatus(String status);

    @Override
    List<Alert> findBySeverity(String severity);

    @Override
    List<Alert> findByIndicatorId(Long indicatorId);

    @Override
    @Query("SELECT a FROM Alert a WHERE a.severity = :severity AND a.status != 'RESOLVED'")
    List<Alert> findUnresolvedBySeverity(String severity);

    @Override
    long countByStatus(String status);

    @Override
    long countBySeverityAndStatus(String severity, String status);
}
