package com.sism.service;

import com.sism.entity.*;
import com.sism.enums.AlertSeverity;
import com.sism.enums.AlertStatus;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.ReportStatus;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.*;

import java.util.stream.Collectors;
import com.sism.vo.AlertEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for alert management
 * Provides alert calculation, event generation, querying, and handling
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertService {

    private final AlertEventRepository alertEventRepository;
    private final AlertWindowRepository alertWindowRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final IndicatorRepository indicatorRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    /**
     * Calculate alert gap percentage for an indicator
     * gap_percent = expected_percent - actual_percent
     * 
     * Requirements: 6.1 - Calculate difference between expected and actual completion
     * 
     * @param indicator the indicator to calculate gap for
     * @param window the alert window for expected progress calculation
     * @return the gap percentage (positive means behind schedule)
     */
    public BigDecimal calculateGapPercent(Indicator indicator, AlertWindow window) {
        BigDecimal expectedPercent = calculateExpectedPercent(indicator, window);
        BigDecimal actualPercent = calculateActualPercent(indicator);
        
        return expectedPercent.subtract(actualPercent).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate expected completion percentage based on time elapsed
     * Uses milestone due dates and weights to determine expected progress
     *
     * @param indicator the indicator
     * @param window the alert window
     * @return expected completion percentage
     */
    public BigDecimal calculateExpectedPercent(Indicator indicator, AlertWindow window) {
        List<Milestone> milestones = indicator.getMilestones();
        if (milestones == null || milestones.isEmpty()) {
            return BigDecimal.ZERO;
        }

        LocalDate cutoffDate = window.getCutoffDate();
        // Note: weight_percent field has been removed from milestone table
        // Expected progress calculation now uses milestone completion count
        long completedMilestones = milestones.stream()
                .filter(m -> m.getStatus() == com.sism.enums.MilestoneStatus.COMPLETED)
                .count();

        if (milestones.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(completedMilestones * 100.0 / milestones.size())
                .setScale(2, RoundingMode.HALF_UP);
    }


    /**
     * Calculate actual completion percentage from approved reports
     * 
     * @param indicator the indicator
     * @return actual completion percentage
     */
    public BigDecimal calculateActualPercent(Indicator indicator) {
        // Get the latest approved final report for the indicator
        List<ProgressReport> approvedReports = reportRepository
                .findByIndicator_IndicatorIdAndStatus(indicator.getIndicatorId(), ReportStatus.APPROVED);
        
        if (approvedReports.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Find the report with the highest percent_complete
        return approvedReports.stream()
                .map(ProgressReport::getPercentComplete)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generate alert events for all indicators in a cycle based on an alert window
     * Requirements: 6.1, 6.2 - Generate alerts when gap exceeds threshold
     * 
     * @param windowId the alert window ID
     * @return list of generated alert events
     */
    @Transactional
    public List<AlertEventVO> generateAlertsForWindow(Long windowId) {
        AlertWindow window = alertWindowRepository.findById(windowId)
                .orElseThrow(() -> new ResourceNotFoundException("AlertWindow", windowId));

        Long cycleId = window.getCycle().getCycleId();
        
        // Get all enabled alert rules for this cycle, ordered by threshold ascending
        List<AlertRule> rules = alertRuleRepository.findEnabledByCycleOrderedByThreshold(cycleId);
        if (rules.isEmpty()) {
            log.info("No enabled alert rules found for cycle {}", cycleId);
            return Collections.emptyList();
        }

        // Get all active indicators for this cycle
        // Note: Indicator status filtering now requires custom query
        List<Indicator> indicators = indicatorRepository.findAll().stream()
                .filter(i -> !i.getIsDeleted() && (i.getStatus() == IndicatorStatus.ACTIVE || i.getStatus() == IndicatorStatus.DISTRIBUTED))
                .collect(Collectors.toList());
        
        List<AlertEvent> generatedEvents = new ArrayList<>();

        for (Indicator indicator : indicators) {
            try {
                AlertEvent event = generateAlertForIndicator(indicator, window, rules);
                if (event != null) {
                    generatedEvents.add(event);
                }
            } catch (Exception e) {
                log.error("Failed to generate alert for indicator {}: {}", 
                        indicator.getIndicatorId(), e.getMessage());
            }
        }

        log.info("Generated {} alert events for window {}", generatedEvents.size(), windowId);
        return generatedEvents.stream()
                .map(this::toAlertEventVO)
                .collect(Collectors.toList());
    }

    /**
     * Generate alert event for a single indicator
     * Requirements: 6.2 - Generate alert with appropriate severity based on gap
     * Requirements: 6.3 - Generate missing report alert if no reports exist
     * 
     * @param indicator the indicator
     * @param window the alert window
     * @param rules the alert rules ordered by threshold
     * @return generated alert event or null if no alert needed
     */
    @Transactional
    public AlertEvent generateAlertForIndicator(Indicator indicator, AlertWindow window, List<AlertRule> rules) {
        BigDecimal expectedPercent = calculateExpectedPercent(indicator, window);
        BigDecimal actualPercent = calculateActualPercent(indicator);
        BigDecimal gapPercent = expectedPercent.subtract(actualPercent).setScale(2, RoundingMode.HALF_UP);

        // Check if there are any reports for this indicator
        boolean hasReports = reportRepository.countByIndicator_IndicatorId(indicator.getIndicatorId()) > 0;
        
        // Find the applicable rule based on gap percentage
        AlertRule applicableRule = findApplicableRule(gapPercent, rules);
        
        if (applicableRule == null && hasReports) {
            // No alert needed - gap is below all thresholds and has reports
            return null;
        }

        // If no reports and expected progress > 0, generate a missing report alert
        if (!hasReports && expectedPercent.compareTo(BigDecimal.ZERO) > 0) {
            // Use the highest severity rule for missing reports
            applicableRule = rules.stream()
                    .max(Comparator.comparing(AlertRule::getGapThreshold))
                    .orElse(rules.get(0));
        }

        if (applicableRule == null) {
            return null;
        }

        // Create alert event
        AlertEvent event = new AlertEvent();
        event.setIndicator(indicator);
        event.setWindow(window);
        event.setRule(applicableRule);
        event.setExpectedPercent(expectedPercent);
        event.setActualPercent(actualPercent);
        event.setGapPercent(gapPercent);
        event.setSeverity(applicableRule.getSeverity());
        event.setStatus(AlertStatus.OPEN);

        // Add detail information
        Map<String, Object> details = new HashMap<>();
        details.put("indicatorDesc", indicator.getIndicatorDesc());
        details.put("targetOrgName", indicator.getTargetOrg().getName());
        details.put("hasReports", hasReports);
        details.put("ruleName", applicableRule.getName());
        event.setDetailJson(details);

        return alertEventRepository.save(event);
    }


    /**
     * Find the applicable alert rule based on gap percentage
     * Returns the rule with the highest threshold that the gap exceeds
     * 
     * @param gapPercent the calculated gap percentage
     * @param rules the alert rules ordered by threshold ascending
     * @return the applicable rule or null if gap is below all thresholds
     */
    private AlertRule findApplicableRule(BigDecimal gapPercent, List<AlertRule> rules) {
        if (gapPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // No gap or ahead of schedule
        }

        AlertRule applicableRule = null;
        for (AlertRule rule : rules) {
            if (gapPercent.compareTo(rule.getGapThreshold()) >= 0) {
                applicableRule = rule;
            }
        }
        return applicableRule;
    }

    /**
     * Get alert events by severity
     * Requirements: 6.4 - Display alerts sorted by severity
     * 
     * @param severity the severity level
     * @param pageable pagination parameters
     * @return page of alert events
     */
    public Page<AlertEventVO> getAlertsBySeverity(AlertSeverity severity, Pageable pageable) {
        Page<AlertEvent> events = alertEventRepository.findBySeverity(severity, pageable);
        return events.map(this::toAlertEventVO);
    }

    /**
     * Get alert events by status
     * Requirements: 6.4 - Display alerts sorted by status
     * 
     * @param status the alert status
     * @param pageable pagination parameters
     * @return page of alert events
     */
    public Page<AlertEventVO> getAlertsByStatus(AlertStatus status, Pageable pageable) {
        Page<AlertEvent> events = alertEventRepository.findByStatus(status, pageable);
        return events.map(this::toAlertEventVO);
    }

    /**
     * Get alert events by severity and status
     * Requirements: 6.4 - Filter alerts by severity and status
     * 
     * @param severity the severity level
     * @param status the alert status
     * @param pageable pagination parameters
     * @return page of alert events
     */
    public Page<AlertEventVO> getAlertsBySeverityAndStatus(AlertSeverity severity, AlertStatus status, 
                                                            Pageable pageable) {
        Page<AlertEvent> events = alertEventRepository.findBySeverityAndStatus(severity, status, pageable);
        return events.map(this::toAlertEventVO);
    }

    /**
     * Get all open alerts sorted by severity and time
     * Requirements: 6.4 - Display alerts sorted by severity level and time
     * 
     * @param pageable pagination parameters
     * @return page of open alert events
     */
    public Page<AlertEventVO> getOpenAlerts(Pageable pageable) {
        Page<AlertEvent> events = alertEventRepository.findByStatusOrderByCreatedAtDesc(AlertStatus.OPEN, pageable);
        
        // Sort by severity (CRITICAL > WARNING > INFO) then by created time
        List<AlertEventVO> sortedList = events.getContent().stream()
                .map(this::toAlertEventVO)
                .sorted(Comparator
                        .comparing(AlertEventVO::getSeverity, this::compareSeverity)
                        .thenComparing(AlertEventVO::getCreatedAt, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return new PageImpl<>(sortedList, pageable, events.getTotalElements());
    }

    /**
     * Compare severity levels (CRITICAL > WARNING > INFO)
     */
    private int compareSeverity(AlertSeverity s1, AlertSeverity s2) {
        Map<AlertSeverity, Integer> order = Map.of(
                AlertSeverity.CRITICAL, 0,
                AlertSeverity.WARNING, 1,
                AlertSeverity.INFO, 2
        );
        return order.getOrDefault(s1, 3).compareTo(order.getOrDefault(s2, 3));
    }

    /**
     * Get critical open alerts
     * 
     * @param pageable pagination parameters
     * @return page of critical open alerts
     */
    public Page<AlertEventVO> getCriticalOpenAlerts(Pageable pageable) {
        Page<AlertEvent> events = alertEventRepository.findCriticalOpenAlerts(pageable);
        return events.map(this::toAlertEventVO);
    }

    /**
     * Get alerts by indicator ID
     * 
     * @param indicatorId the indicator ID
     * @param pageable pagination parameters
     * @return page of alert events for the indicator
     */
    public Page<AlertEventVO> getAlertsByIndicator(Long indicatorId, Pageable pageable) {
        Page<AlertEvent> events = alertEventRepository.findByIndicator_IndicatorId(indicatorId, pageable);
        return events.map(this::toAlertEventVO);
    }

    /**
     * Get alerts by target organization
     *
     * @param orgId the organization ID
     * @param pageable pagination parameters
     * @return page of alert events for the organization
     */
    public Page<AlertEventVO> getAlertsByTargetOrg(Long orgId, Pageable pageable) {
        // Note: AlertEvent doesn't have direct targetOrg field
        // Need to join through Indicator -> targetOrg
        // This query should be implemented in AlertEventRepository
        return Page.empty(pageable);
    }

    /**
     * Get alert event by ID
     * 
     * @param eventId the event ID
     * @return alert event VO
     */
    public AlertEventVO getAlertById(Long eventId) {
        AlertEvent event = alertEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("AlertEvent", eventId));
        return toAlertEventVO(event);
    }


    /**
     * Handle an alert event
     * Requirements: 6.5 - Record handler and handling notes, close the alert
     * 
     * @param eventId the event ID
     * @param handledById the user ID of the handler
     * @param handledNote the handling notes
     * @return updated alert event VO
     */
    @Transactional
    public AlertEventVO handleAlert(Long eventId, Long handledById, String handledNote) {
        AlertEvent event = alertEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("AlertEvent", eventId));

        // Validate that alert is not already closed
        if (event.getStatus() == AlertStatus.CLOSED || event.getStatus() == AlertStatus.RESOLVED) {
            throw new BusinessException("Alert event is already closed or resolved");
        }

        SysUser handler = userRepository.findById(handledById)
                .orElseThrow(() -> new ResourceNotFoundException("User", handledById));

        event.setHandledBy(handler);
        event.setHandledNote(handledNote);
        event.setStatus(AlertStatus.RESOLVED);

        AlertEvent savedEvent = alertEventRepository.save(event);
        log.info("Alert event {} handled by user {}", eventId, handledById);

        return toAlertEventVO(savedEvent);
    }

    /**
     * Close an alert event
     * Requirements: 6.5 - Close the alert
     * 
     * @param eventId the event ID
     * @param handledById the user ID of the handler
     * @param handledNote the closing notes
     * @return updated alert event VO
     */
    @Transactional
    public AlertEventVO closeAlert(Long eventId, Long handledById, String handledNote) {
        AlertEvent event = alertEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("AlertEvent", eventId));

        if (event.getStatus() == AlertStatus.CLOSED) {
            throw new BusinessException("Alert event is already closed");
        }

        SysUser handler = userRepository.findById(handledById)
                .orElseThrow(() -> new ResourceNotFoundException("User", handledById));

        event.setHandledBy(handler);
        event.setHandledNote(handledNote);
        event.setStatus(AlertStatus.CLOSED);

        AlertEvent savedEvent = alertEventRepository.save(event);
        log.info("Alert event {} closed by user {}", eventId, handledById);

        return toAlertEventVO(savedEvent);
    }

    /**
     * Update alert status to IN_PROGRESS
     * 
     * @param eventId the event ID
     * @param handledById the user ID starting to handle
     * @return updated alert event VO
     */
    @Transactional
    public AlertEventVO startHandlingAlert(Long eventId, Long handledById) {
        AlertEvent event = alertEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("AlertEvent", eventId));

        if (event.getStatus() != AlertStatus.OPEN) {
            throw new BusinessException("Can only start handling OPEN alerts");
        }

        SysUser handler = userRepository.findById(handledById)
                .orElseThrow(() -> new ResourceNotFoundException("User", handledById));

        event.setHandledBy(handler);
        event.setStatus(AlertStatus.IN_PROGRESS);

        AlertEvent savedEvent = alertEventRepository.save(event);
        log.info("Alert event {} started handling by user {}", eventId, handledById);

        return toAlertEventVO(savedEvent);
    }

    /**
     * Get alert statistics
     * 
     * @return map of statistics
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalOpen", alertEventRepository.countByStatus(AlertStatus.OPEN));
        stats.put("totalInProgress", alertEventRepository.countByStatus(AlertStatus.IN_PROGRESS));
        stats.put("totalResolved", alertEventRepository.countByStatus(AlertStatus.RESOLVED));
        stats.put("totalClosed", alertEventRepository.countByStatus(AlertStatus.CLOSED));
        
        stats.put("criticalOpen", alertEventRepository.countBySeverityAndStatus(AlertSeverity.CRITICAL, AlertStatus.OPEN));
        stats.put("warningOpen", alertEventRepository.countBySeverityAndStatus(AlertSeverity.WARNING, AlertStatus.OPEN));
        stats.put("infoOpen", alertEventRepository.countBySeverityAndStatus(AlertSeverity.INFO, AlertStatus.OPEN));

        return stats;
    }

    /**
     * Convert AlertEvent entity to AlertEventVO
     */
    private AlertEventVO toAlertEventVO(AlertEvent event) {
        AlertEventVO vo = new AlertEventVO();
        vo.setEventId(event.getEventId());
        vo.setIndicatorId(event.getIndicator().getIndicatorId());
        vo.setIndicatorDesc(event.getIndicator().getIndicatorDesc());
        vo.setTargetOrgId(event.getIndicator().getTargetOrg().getId());
        vo.setTargetOrgName(event.getIndicator().getTargetOrg().getName());
        vo.setWindowId(event.getWindow().getWindowId());
        vo.setWindowName(event.getWindow().getName());
        vo.setCutoffDate(event.getWindow().getCutoffDate());
        vo.setRuleId(event.getRule().getRuleId());
        vo.setRuleName(event.getRule().getName());
        vo.setExpectedPercent(event.getExpectedPercent());
        vo.setActualPercent(event.getActualPercent());
        vo.setGapPercent(event.getGapPercent());
        vo.setSeverity(event.getSeverity());
        vo.setStatus(event.getStatus());
        
        if (event.getHandledBy() != null) {
            vo.setHandledById(event.getHandledBy().getId());
            vo.setHandledByName(event.getHandledBy().getRealName());
        }
        vo.setHandledNote(event.getHandledNote());
        vo.setDetailJson(event.getDetailJson());
        vo.setCreatedAt(event.getCreatedAt());
        vo.setUpdatedAt(event.getUpdatedAt());

        return vo;
    }
}
