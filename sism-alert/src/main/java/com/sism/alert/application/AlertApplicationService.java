package com.sism.alert.application;

import com.sism.alert.domain.Alert;
import com.sism.alert.domain.repository.AlertRepository;
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

    // ==================== Create ====================

    @Transactional
    public Alert createAlert(Long indicatorId, Long ruleId, Long windowId,
                            String severity, BigDecimal actualPercent,
                            BigDecimal expectedPercent, BigDecimal gapPercent,
                            String detailJson) {
        Alert alert = new Alert();
        alert.setIndicatorId(indicatorId);
        alert.setRuleId(ruleId);
        alert.setWindowId(windowId);
        alert.setSeverity(severity);
        alert.setActualPercent(actualPercent);
        alert.setExpectedPercent(expectedPercent);
        alert.setGapPercent(gapPercent);
        alert.setDetailJson(detailJson);
        alert.setStatus(Alert.STATUS_PENDING);
        alert.validate();
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert triggerAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alert.trigger();
        return alertRepository.save(alert);
    }

    // ==================== Read ====================

    public Optional<Alert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public List<Alert> getAlertsByStatus(String status) {
        return alertRepository.findByStatus(status);
    }

    public List<Alert> getAlertsBySeverity(String severity) {
        return alertRepository.findBySeverity(severity);
    }

    public List<Alert> getAlertsByIndicatorId(Long indicatorId) {
        return alertRepository.findByIndicatorId(indicatorId);
    }

    public List<Alert> getUnresolvedAlerts() {
        return alertRepository.findByStatus(Alert.STATUS_TRIGGERED);
    }

    // ==================== Update ====================

    @Transactional
    public Alert resolveAlert(Long alertId, Long handledBy, String handledNote) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alert.resolve(handledBy, handledNote);
        return alertRepository.save(alert);
    }

    // ==================== Statistics ====================

    public long countAlerts() {
        return alertRepository.count();
    }

    public long countByStatus(String status) {
        return alertRepository.countByStatus(status);
    }

    public long countBySeverity(String severity) {
        return alertRepository.countBySeverityAndStatus(severity, Alert.STATUS_TRIGGERED);
    }

    /**
     * Get alert statistics: totalOpen + countBySeverity breakdown
     */
    public Map<String, Object> getAlertStats() {
        long totalOpen = alertRepository.countByStatus(Alert.STATUS_TRIGGERED)
                + alertRepository.countByStatus(Alert.STATUS_PENDING);

        Map<String, Long> countBySeverity = new LinkedHashMap<>();
        countBySeverity.put("CRITICAL", alertRepository.countBySeverityAndStatus("CRITICAL", Alert.STATUS_TRIGGERED)
                + alertRepository.countBySeverityAndStatus("CRITICAL", Alert.STATUS_PENDING));
        countBySeverity.put("MAJOR", alertRepository.countBySeverityAndStatus("MAJOR", Alert.STATUS_TRIGGERED)
                + alertRepository.countBySeverityAndStatus("MAJOR", Alert.STATUS_PENDING));
        countBySeverity.put("MINOR", alertRepository.countBySeverityAndStatus("MINOR", Alert.STATUS_TRIGGERED)
                + alertRepository.countBySeverityAndStatus("MINOR", Alert.STATUS_PENDING));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalOpen", totalOpen);
        stats.put("countBySeverity", countBySeverity);
        return stats;
    }
}
