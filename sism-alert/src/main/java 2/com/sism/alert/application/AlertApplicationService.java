package com.sism.alert.application;

import com.sism.alert.domain.Alert;
import com.sism.alert.domain.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * AlertApplicationService - 预警应用服务
 * 负责预警相关的业务操作
 */
@Service
@RequiredArgsConstructor
public class AlertApplicationService {

    private final AlertRepository alertRepository;

    // ==================== Create ====================

    /**
     * 创建预警
     */
    @Transactional
    public Alert createAlert(String alertType, String title, String description,
                            String severity, String entityType, Long entityId) {
        Alert alert = new Alert();
        alert.setAlertType(alertType);
        alert.setTitle(title);
        alert.setDescription(description);
        alert.setSeverity(severity);
        alert.setEntityType(entityType);
        alert.setEntityId(entityId);
        alert.setStatus(Alert.STATUS_PENDING);
        alert.validate();
        return alertRepository.save(alert);
    }

    /**
     * 触发预警
     */
    @Transactional
    public Alert triggerAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alert.trigger();
        return alertRepository.save(alert);
    }

    // ==================== Read ====================

    /**
     * 根据ID查询预警
     */
    public Optional<Alert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }

    /**
     * 查询所有预警
     */
    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    /**
     * 根据状态查询预警
     */
    public List<Alert> getAlertsByStatus(String status) {
        return alertRepository.findByStatus(status);
    }

    /**
     * 根据类型查询预警
     */
    public List<Alert> getAlertsByType(String alertType) {
        return alertRepository.findByAlertType(alertType);
    }

    /**
     * 根据严重程度查询预警
     */
    public List<Alert> getAlertsBySeverity(String severity) {
        return alertRepository.findBySeverity(severity);
    }

    /**
     * 根据实体查询预警
     */
    public List<Alert> getAlertsByEntity(String entityType, Long entityId) {
        return alertRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * 查询未解决的预警
     */
    public List<Alert> getUnresolvedAlerts() {
        return alertRepository.findByStatus(Alert.STATUS_TRIGGERED);
    }

    // ==================== Update ====================

    /**
     * 确认预警
     */
    @Transactional
    public Alert resolveAlert(Long alertId, Long resolvedBy, String resolution) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alert.resolve(resolvedBy, resolution);
        return alertRepository.save(alert);
    }

    // ==================== Statistics ====================

    /**
     * 统计预警数量
     */
    public long countAlerts() {
        return alertRepository.count();
    }

    /**
     * 根据状态统计
     */
    public long countByStatus(String status) {
        return alertRepository.countByStatus(status);
    }

    /**
     * 根据严重程度统计
     */
    public long countBySeverity(String severity) {
        return alertRepository.countBySeverityAndStatus(severity, Alert.STATUS_TRIGGERED);
    }
}
