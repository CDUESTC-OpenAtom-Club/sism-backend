package com.sism.alert.application;

import com.sism.alert.domain.Alert;
import com.sism.alert.domain.enums.AlertSeverity;
import com.sism.alert.domain.enums.AlertStatus;
import com.sism.alert.domain.repository.AlertRepository;
import com.sism.alert.interfaces.dto.AlertStatsDTO;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        AlertSeverity normalizedSeverity = AlertSeverity.normalize(severity);
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
        AlertSeverity normalizedSeverity = AlertSeverity.normalize(severity);
        if (normalizedSeverity == null) {
            return List.of();
        }
        return alertRepository.findBySeverity(normalizedSeverity);
    }

    public List<Alert> getAlertsByIndicatorId(Long indicatorId) {
        return alertRepository.findByIndicatorId(indicatorId);
    }

    public List<Alert> getUnresolvedAlerts() {
        return alertRepository.findByStatusIn(List.of(AlertStatus.OPEN, AlertStatus.IN_PROGRESS));
    }

    public Page<Alert> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable);
    }

    public Page<Alert> getAlertsByStatus(String status, Pageable pageable) {
        AlertStatus normalizedStatus = Alert.normalizeStatus(status);
        if (normalizedStatus == null) {
            return Page.empty(pageable);
        }
        return alertRepository.findByStatus(normalizedStatus, pageable);
    }

    public Page<Alert> getAlertsBySeverity(String severity, Pageable pageable) {
        AlertSeverity normalizedSeverity = AlertSeverity.normalize(severity);
        if (normalizedSeverity == null) {
            return Page.empty(pageable);
        }
        return alertRepository.findBySeverity(normalizedSeverity, pageable);
    }

    public Page<Alert> getAlertsByIndicatorId(Long indicatorId, Pageable pageable) {
        return alertRepository.findByIndicatorId(indicatorId, pageable);
    }

    public Page<Alert> getUnresolvedAlerts(Pageable pageable) {
        return alertRepository.findByStatusIn(List.of(AlertStatus.OPEN, AlertStatus.IN_PROGRESS), pageable);
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
        AlertSeverity normalizedSeverity = AlertSeverity.normalize(severity);
        if (normalizedSeverity == null) {
            return 0L;
        }
        return alertRepository.countBySeverity(normalizedSeverity);
    }

    /**
     * Get alert statistics: totalOpen + countBySeverity breakdown
     */
    public AlertStatsDTO getAlertStats() {
        Map<String, Long> countBySeverity = toSeverityCountMap(alertRepository.countOpenBySeverity());
        long totalOpen = countBySeverity.values().stream().mapToLong(Long::longValue).sum();
        return new AlertStatsDTO(totalOpen, countBySeverity);
    }

    private Map<String, Long> toSeverityCountMap(List<AlertRepository.SeverityCount> counts) {
        Map<String, Long> countBySeverity = new LinkedHashMap<>();
        countBySeverity.put(AlertSeverity.CRITICAL.name(), 0L);
        countBySeverity.put(AlertSeverity.WARNING.name(), 0L);
        countBySeverity.put(AlertSeverity.INFO.name(), 0L);
        for (AlertRepository.SeverityCount count : counts) {
            if (count.getSeverity() != null) {
                countBySeverity.put(count.getSeverity().name(), count.getCount());
            }
        }
        return countBySeverity;
    }

    private void publishAndClearEvents(Alert alert) {
        List<DomainEvent> events = alert.getDomainEvents();
        eventPublisher.publishAll(events);
        alert.clearEvents();
    }
}
