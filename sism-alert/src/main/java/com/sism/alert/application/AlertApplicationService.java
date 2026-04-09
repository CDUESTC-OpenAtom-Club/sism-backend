package com.sism.alert.application;

import com.sism.alert.domain.Alert;
import com.sism.alert.domain.enums.AlertSeverity;
import com.sism.alert.domain.enums.AlertStatus;
import com.sism.alert.domain.repository.AlertRepository;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AlertApplicationService - 预警应用服务
 * 负责预警相关的业务操作
 */
@Service
@RequiredArgsConstructor
public class AlertApplicationService {

    private final AlertRepository alertRepository;
    private final DomainEventPublisher eventPublisher;

    // ==================== Create ====================

    @Transactional
    public Alert createAlert(Long indicatorId, Long ruleId, Long windowId,
                            String severity, BigDecimal actualPercent,
                            BigDecimal expectedPercent, BigDecimal gapPercent,
                            String detailJson) {
        String normalizedSeverity = AlertSeverity.normalize(severity);
        if (normalizedSeverity == null) {
            throw new IllegalArgumentException("Severity must be INFO, WARNING, or CRITICAL");
        }
        Alert alert = new Alert();
        alert.setIndicatorId(indicatorId);
        alert.setRuleId(ruleId);
        alert.setWindowId(windowId);
        alert.setSeverity(normalizedSeverity);
        alert.setActualPercent(actualPercent);
        alert.setExpectedPercent(expectedPercent);
        alert.setGapPercent(gapPercent);
        alert.setDetailJson(detailJson);
        alert.setStatus(AlertStatus.OPEN);
        alert.validate();
        Alert saved = alertRepository.save(alert);
        saved.recordCreated();
        publishAndClearEvents(saved);
        return saved;
    }

    @Transactional
    public Alert triggerAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.trigger();
        Alert saved = alertRepository.save(alert);
        publishAndClearEvents(saved);
        return saved;
    }

    // ==================== Read ====================

    public Optional<Alert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public List<Alert> getAlertsByStatus(String status) {
        AlertStatus normalizedStatus = Alert.normalizeStatus(status);
        if (normalizedStatus == null) {
            return List.of();
        }
        return alertRepository.findByStatus(normalizedStatus);
    }

    public List<Alert> getAlertsBySeverity(String severity) {
        String normalizedSeverity = AlertSeverity.normalize(severity);
        if (normalizedSeverity == null) {
            return List.of();
        }
        return alertRepository.findBySeverity(normalizedSeverity);
    }

    public List<Alert> getAlertsByIndicatorId(Long indicatorId) {
        return alertRepository.findByIndicatorId(indicatorId);
    }

    public List<Alert> getUnresolvedAlerts() {
        List<Alert> unresolvedAlerts = new java.util.ArrayList<>();
        unresolvedAlerts.addAll(alertRepository.findByStatus(AlertStatus.OPEN));
        unresolvedAlerts.addAll(alertRepository.findByStatus(AlertStatus.IN_PROGRESS));
        return unresolvedAlerts;
    }

    // ==================== Update ====================

    @Transactional
    public Alert resolveAlert(Long alertId, Long handledBy, String handledNote) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.resolve(handledBy, handledNote);
        Alert saved = alertRepository.save(alert);
        publishAndClearEvents(saved);
        return saved;
    }

    @Transactional
    public void deleteAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alertRepository.delete(alert);
    }

    // ==================== Statistics ====================

    public long countAlerts() {
        return alertRepository.count();
    }

    public long countByStatus(String status) {
        AlertStatus normalizedStatus = Alert.normalizeStatus(status);
        if (normalizedStatus == null) {
            return 0L;
        }
        return alertRepository.countByStatus(normalizedStatus);
    }

    public long countBySeverity(String severity) {
        String normalizedSeverity = AlertSeverity.normalize(severity);
        if (normalizedSeverity == null) {
            return 0L;
        }
        return alertRepository.countBySeverity(normalizedSeverity);
    }

    /**
     * Get alert statistics: totalOpen + countBySeverity breakdown
     */
    public Map<String, Object> getAlertStats() {
        long totalOpen = alertRepository.countByStatus(AlertStatus.IN_PROGRESS)
                + alertRepository.countByStatus(AlertStatus.OPEN);

        Map<String, Long> countBySeverity = new LinkedHashMap<>();
        countBySeverity.put("CRITICAL", countBySeverityAndOpenStatus("CRITICAL"));
        countBySeverity.put("WARNING", countBySeverityAndOpenStatus("WARNING"));
        countBySeverity.put("INFO", countBySeverityAndOpenStatus("INFO"));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalOpen", totalOpen);
        stats.put("countBySeverity", countBySeverity);
        return stats;
    }

    private long countBySeverityAndOpenStatus(String severity) {
        String normalizedSeverity = AlertSeverity.normalize(severity);
        if (normalizedSeverity == null) {
            return 0L;
        }
        return alertRepository.countBySeverityAndStatus(normalizedSeverity, AlertStatus.IN_PROGRESS)
                + alertRepository.countBySeverityAndStatus(normalizedSeverity, AlertStatus.OPEN);
    }

    private void publishAndClearEvents(Alert alert) {
        List<DomainEvent> events = alert.getDomainEvents();
        eventPublisher.publishAll(events);
        alert.clearEvents();
    }
}
